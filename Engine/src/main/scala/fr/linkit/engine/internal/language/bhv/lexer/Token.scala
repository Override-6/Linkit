/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.lexer

import scala.util.matching.Regex

trait Token {
    val value: String

    override def toString: String = value
}

trait Keyword extends Token
trait Symbol extends Token

object Symbol {
    def makeRegex(tokens: Array[Symbol]): Regex = {
       val r = tokens.sortBy(_.value.length)
               .reverse
               .map(_.value.map("\\" + _).mkString("(", "", ")"))
               .mkString("|").r
        r
    }
}

abstract class Value(override val value: String) extends Token
