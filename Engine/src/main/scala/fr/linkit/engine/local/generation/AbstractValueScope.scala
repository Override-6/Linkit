/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.local.generation

import fr.linkit.api.local.generation._
import fr.linkit.engine.local.generation.AbstractValueScope.FlowControllerBlock
import fr.linkit.engine.local.generation.controllers.BPController
import fr.linkit.engine.local.generation.controllers.BPController.Else

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class AbstractValueScope[A](override val name: String,
                                     override val position: Int,
                                     upperBlueprint: String) extends ValueScope[A] {

    private val subBlocks  = {
        LexerUtils
                .expressionsBetween("\\$\\$", "\\$\\$", upperBlueprint)
                .map(getScopeBlock)
    }
    private val conditions = findFlowControllers()
    private val values     = mutable.Map.empty[String, BlueprintValue[A]]
    private val subScopes  = mutable.Map.empty[String, SubScopeCategory[_]]

    def getSourceCode(value: A): String = {
        val inserter = new BlueprintInserter(0, upperBlueprint)
        conditions.foreach(_.insertResult(inserter, value))
        insertValues(inserter, value)
        inserter.getResult
    }

    def insertValues(inserter: BlueprintInserter, value: A): Unit = {
        values.values.foreach(_.replaceValues(inserter, value))
        subScopes.values.foreach(_.insertResult(inserter, value))
    }

    private def getScopeBlock(pair: (String, Int)): ScopeBlock = {
        val (name, pos) = pair
        ScopeBlock(name, pos, LexerUtils.nextBlock(upperBlueprint, pos))
    }

    protected def registerValue(pair: (String, A => String)): Unit = {
        values.put(pair._1, BlueprintValueSupplier[A](pair)(upperBlueprint))
    }

    protected def bindSubScope[B](scopeFactory: (String, Int) => ValueScope[B], valueIterator: ValueIterator[A, B]): Unit = {
        val scopes = subBlocks.map(block => (scopeFactory(block.blockBlueprint, block.startPos), block))
        if (scopes.isEmpty) {
            throw new NoSuchElementException("Sub scope not present.")
        }
        val name = scopes.head._2.name
        subScopes.put(name, new SubScopeCategory[B](scopes, valueIterator))
    }

    private def isPositionOwnedBySubScope(position: Int): Boolean = {
        subBlocks.exists(block => {
            val start = block.startPos
            val end   = start + block.blockBlueprint.length
            position >= start && position <= end
        })
    }

    private def findFlowControllers(): Array[FlowControllerBlock[A]] = {
        val expressions                     = LexerUtils.expressionsBetween("\\$\\|", "\\|\\$", upperBlueprint)
        val controllers                     = ListBuffer.empty[FlowControllerBlock[A]]
        var before: BlueprintFlowController = null //iteration over controller is reversed
        var index                           = 0
        for ((expression, position) <- expressions) {
            val blockBlueprint = LexerUtils.nextBlock(upperBlueprint, position)
            val exprEnd        = position + expression.length + 4 //adding "$||$" quotes
            val blockStartPos  = upperBlueprint.indexWhere(!_.isWhitespace, exprEnd) + 2 // adding '{' and '}' suppression

            findExpression(expression, position, blockStartPos, blockBlueprint)
        }

        def findExpression(expression: String, exprPos: Int, blockStartPos: Int, blockBlueprint: String): Unit = {

            if (isPositionOwnedBySubScope(exprPos))
                return

            val arguments = expression.split("\\s")
            val kind      = arguments(0).toLowerCase
            kind match {
                case "if"   =>
                    before = BPController.IfElif(blockStartPos, exprPos, blockBlueprint, arguments.drop(1), null)
                case "elif" =>
                    if (before == null)
                        throw new IllegalArgumentException("Illegal head elif expression.")
                    before = BPController.IfElif(blockStartPos, exprPos, blockBlueprint, arguments.drop(1), before)
                case "else" =>
                    if (before == null)
                        throw new IllegalArgumentException("Illegal head else expression.")
                    before = BPController.Else(blockStartPos, exprPos, blockBlueprint, before)
                case _      => throw new IllegalArgumentException(s"Unknown expression '$expression' for flow controllers.")
            }
            conclude(blockStartPos, blockBlueprint)
        }

        def conclude(blockStartPos: Int, blockBlueprint: String): Unit = {
            if (index + 1 < expressions.length) {
                def nextPosition: Int = expressions(index + 1)._2

                if (upperBlueprint.indexWhere(!_.isWhitespace, blockStartPos + blockBlueprint.length) != nextPosition
                        || (before != null && before.isInstanceOf[Else])) {
                    if (before != null)
                        controllers += FlowControllerBlock(before, this)
                    before = null //start a new condition chain expression.
                }
                index += 1
            } else {
                controllers += FlowControllerBlock(before, this)
            }
        }

        controllers.toArray
    }

    implicit protected class Helper(private val self: String) {

        @inline
        def ~>(supplier: A => String): (String, A => String) = {
            self -> supplier
        }

        @inline
        def ~~>[T](supplier: T): (String, A => String) = {
            self -> (_ => supplier.toString)
        }
    }

    case class ScopeBlock(name: String, startPos: Int, blockBlueprint: String) {
        val blockLength: Int = {
            upperBlueprint.indexOf('{', startPos) - startPos + blockBlueprint.length + 2
        }
    }

    class SubScopeCategory[B](scopes: Seq[(ValueScope[B], ScopeBlock)], iterator: ValueIterator[A, B]) {

        def insertResult(inserter: BlueprintInserter, value: A): Unit = {

            scopes.foreach(pair => {
                val scope = pair._1
                val block = pair._2
                inserter.deleteBlock(block.startPos, block.blockLength)
                iterator.foreach(value, { subValue =>
                    val sourceCode = scope.getSourceCode(subValue)
                    val start      = sourceCode.indexOf('{', scope.name.length + 4) + 1 // +4 to remove '$$' quotes + 1 to remove first '{'
                    val end        = sourceCode.lastIndexOf('}') //Removing last '}'
                    val blueprint = sourceCode.slice(start, end)
                    inserter.insertBlock(blueprint, scope.position, blueprint.length) // Removing last '}' char
                })
            })
        }
    }
}

object AbstractValueScope {

    case class FlowControllerBlock[A](control: BlueprintFlowController, scope: AbstractValueScope[A]) {

        private val (startPos, totalLength) = getDimensions

        def insertResult(upperInserter: BlueprintInserter, value: A): Unit = {
            val (blueprint, pos) = control.getBlueprint(name => {
                scope.values
                        .get(name)
                        .map(_.getValue(value))
                        .getOrElse(throw new NoSuchElementException(s"Unknown value '$name'"))
            })
            val inserter         = new BlueprintInserter(pos - 1, blueprint)
            upperInserter.deleteBlock(startPos, totalLength)
            scope.insertValues(inserter, value)
            upperInserter.insertBlock(inserter.getResult, startPos, totalLength)
        }


        private def getDimensions: (Int, Int) = {
            var startPos    = 0
            var totalLength = 0
            var lastControl = control
            while (lastControl != null) {
                startPos = lastControl.expressionPosition
                totalLength += (lastControl.blockPosition - lastControl.expressionPosition) + lastControl.layerBlueprint.length
                lastControl = lastControl.before
            }
            (startPos, totalLength + 1)
        }
    }

}