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

package fr.linkit.engine.internal.language.cbp

import fr.linkit.api.internal.language.cbp._
import fr.linkit.engine.internal.language.cbp.AbstractValueScope.FlowControllerChain
import fr.linkit.engine.internal.language.cbp.controllers.BPController
import fr.linkit.engine.internal.language.cbp.controllers.BPController.Else

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class AbstractValueScope[A](override val name: String,
                                     override val position: Int,
                                     upperBlueprint: String) extends ValueScope[A] {

    private val subBlocks  = {
        LexerUtils
                .expressionsBetween("!!", "!!", upperBlueprint)
                .map(getScopeBlock)
    }
    private val conditions = findFlowControllers()
    private val values     = mutable.Map.empty[String, BlueprintValue[A]]
    private val subScopes  = mutable.Map.empty[String, AbstractValueScope[A]#SubScopeCategory[_]]
    private val bindingLogics = ListBuffer.empty[AbstractValueScope[A] => Unit]

    def getSourceCode(value: A): String = {
        val inserter = new SimpleValueInserter(0, upperBlueprint)
        conditions.foreach(_.insertResult(inserter, value))
        insertValues(inserter, value)
        inserter.getStringResult
    }

    def insertValues(inserter: ValueInserter, value: A): Unit = {
        values.values.foreach(_.replaceValues(inserter, value))
        subScopes.values.foreach(_.insertResult(inserter, value))
    }

    def bindAll(other: AbstractValueScope[A]): Unit = {
        other.bindingLogics.foreach(_.apply(this))
    }

    private def getScopeBlock(pair: (String, Int)): ScopeBlock = {
        val (name, pos) = pair
        ScopeBlock(name, pos, LexerUtils.nextBlock(upperBlueprint, pos))
    }

    protected def bindValue(pair: (String, A => String)): Unit = {
        bindingLogics += (_.bindValue(pair))
        values.put(pair._1, BlueprintValueSupplier[A](pair)(upperBlueprint))
    }

    protected def bindSubScope[B](scopeFactory: (String, Int) => ValueScope[B], contextIterator: ContextIterator[A, B]): Unit = {
        bindingLogics += (_.bindSubScope(scopeFactory, contextIterator))
        val scopes = subBlocks.map(block => (scopeFactory(block.blockBlueprint, block.startPos), block))
        if (scopes.isEmpty) {
            throw new NoSuchElementException("Sub scope not present.")
        }
        val name = scopes.head._2.name
        subScopes.put(name, new SubScopeCategory[B](scopes, contextIterator))
    }

    private def isPositionOwnedBySubScope(position: Int): Boolean = {
        subBlocks.exists(block => {
            val start = block.startPos
            val end   = start + block.blockBlueprint.length
            position >= start && position <= end
        })
    }

    private def findFlowControllers(): Array[FlowControllerChain[A]] = {
        val expressions                     = LexerUtils.expressionsBetween("\\$\\|", "\\|\\$", upperBlueprint)
        val controllers                     = ListBuffer.empty[FlowControllerChain[A]]
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

            val kind = expression.takeWhile(!_.isWhitespace).toLowerCase
            kind match {
                case "if"   =>
                    before = BPController.IfElif(blockStartPos, exprPos, blockBlueprint, expression.drop(kind.length), null)
                case "elif" =>
                    if (before == null)
                        throw new IllegalArgumentException("Illegal heading elif expression.")
                    before = BPController.IfElif(blockStartPos, exprPos, blockBlueprint, expression.drop(kind.length), before)
                case "else" =>
                    if (before == null)
                        throw new IllegalArgumentException("Illegal heading else expression.")
                    before = BPController.Else(blockStartPos, exprPos, blockBlueprint, before)
                case _      => throw new IllegalArgumentException(s"Unknown flow controller expression '$expression'.")
            }
            conclude(blockStartPos, blockBlueprint)
        }

        def conclude(blockStartPos: Int, blockBlueprint: String): Unit = {
            if (index + 1 < expressions.length) {
                def nextPosition: Int = expressions(index + 1)._2

                if (upperBlueprint.indexWhere(!_.isWhitespace, blockStartPos + blockBlueprint.length) != nextPosition
                        || (before != null && before.isInstanceOf[Else])) {
                    if (before != null)
                        controllers += FlowControllerChain(before, this)
                    before = null //start a new condition chain expression.
                }
                index += 1
            } else {
                controllers += FlowControllerChain(before, this)
            }
        }

        controllers.toArray
    }

    implicit protected class Helper(private val self: String) {

        @inline
        def ~>(@inline supplier: A => String): (String, A => String) = {
            self -> supplier
        }

        @inline
        def ~~>[T](@inline supplier: T): (String, A => String) = {
            self -> (_ => supplier.toString)
        }
    }

    case class ScopeBlock(name: String, startPos: Int, blockBlueprint: String) {

        val blockLength: Int = {
            upperBlueprint.indexOf('{', startPos) - startPos + blockBlueprint.length + 2
        }
    }

    class SubScopeCategory[B](scopes: Seq[(ValueScope[B], ScopeBlock)], iterator: ContextIterator[A, B]) {

        def insertResult(inserter: ValueInserter, value: A): Unit = {

            scopes.foreach(pair => {
                val scope = pair._1
                val block = pair._2
                inserter.deleteBlock(block.startPos, block.blockLength)
                iterator.foreach(value, { subValue =>
                    val sourceCode = scope.getSourceCode(subValue)
                    inserter.insertBlock(sourceCode, scope.position) // Removing last '}' char
                })
            })
        }
    }
}

object AbstractValueScope {

    case class FlowControllerChain[A](control: BlueprintFlowController, scope: AbstractValueScope[A]) {

        private val (startPos, totalLength) = getDimensions

        def insertResult(upperInserter: ValueInserter, value: A): Unit = {
            val result = control.getBlueprint(name => {
                val r = scope.values
                        .get(name)
                        .map(_.getValue(value))
                r.getOrElse(throw new NoSuchElementException(s"Unknown value '$name'"))
            })
            if (result.isEmpty)
                return

            val (blueprint, pos) = result.get
            val inserter         = new SimpleValueInserter(pos - 1, blueprint)
            upperInserter.deleteBlock(startPos, totalLength)
            scope.insertValues(inserter, value)
            upperInserter.insertOther(inserter, startPos)
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