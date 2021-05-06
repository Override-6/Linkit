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

import fr.linkit.api.local.generation.{BlueprintInserter, BlueprintValue, LexerUtils, ValueScope}
import fr.linkit.engine.local.generation.AbstractValueScope.{ScopeBlock, SubScopeCategory}

import scala.collection.mutable

class AbstractValueScope[A](override val name: String,
                            override val position: Int,
                            upperBlueprint: String) extends ValueScope[A] {

    private val subBlocks = {
        LexerUtils
                .positionsBetween("\\$\\$", upperBlueprint)
                .map(getScopeBlock)
    }
    private val values    = mutable.Map.empty[String, BlueprintValue[A]]
    private val subScopes = mutable.Map.empty[String, SubScopeCategory[A, _]]

    def getSourceCode(value: A): String = {
        val inserter = new BlueprintInserter(new StringBuilder(upperBlueprint))
        values.values.foreach(_.replaceValues(inserter, value))
        subScopes.values.foreach(_.insertResult(inserter, value))
        inserter.getResult
    }

    private def getScopeBlock(pair: (String, Int)): ScopeBlock = {
        val (name, pos) = pair
        ScopeBlock(name, pos, LexerUtils.nextBlock(upperBlueprint, pos))
    }

    protected def registerValue(pair: (String, A => String)): Unit = {
        values.put(pair._1, BlueprintValueSupplier[A](pair)(upperBlueprint))
    }

    protected def bindSubScope[B](scopeFactory: (String, Int) => ValueScope[B], transform: A => B): Unit = {
        val scopes = subBlocks.map(block => (scopeFactory(block.blockBlueprint, block.startPos), block))
        if (scopes.isEmpty) {
            throw new NoSuchElementException("Sub scope not present.")
        }
        val name = scopes.head._2.name
        subScopes.put(name, new SubScopeCategory[A, B](scopes, transform))
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

}

object AbstractValueScope {

    class SubScopeCategory[A, B](scopes: Seq[(ValueScope[B], ScopeBlock)], transform: A => B) {

        def insertResult(inserter: BlueprintInserter, value: A): Unit = {

            scopes.foreach(pair => {
                val scope = pair._1
                val block = pair._2
                inserter.deleteBlock(block.startPos, block.blockBlueprint.length)
                var subValue = transform(value)
                while (subValue != null) {
                    val sourceCode = scope.getSourceCode(subValue)
                    val start      = sourceCode.indexOf('{', scope.name.length + 4) + 1 // +4 to remove '$$' quotes + 1 to remove first '{'
                    val end        = sourceCode.lastIndexOf('}') //Removing last '}'
                    inserter.insertBlock(sourceCode.slice(start, end), scope.position) // Removing last '}' char
                    subValue = transform(value)
                }
            })
        }
    }

    case class ScopeBlock(name: String, startPos: Int, blockBlueprint: String)

}