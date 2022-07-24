/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.engine.internal.language.bhv.lexer.Token

import scala.util.parsing.input.{NoPosition, Position, Reader}

class TokenReader[T <: Token]private(tokens: Seq[(T, Position)],
                              override val source: String,
                              val filePath: String) extends Reader[T] {

    def this(context: ParserContext[T]) = {
        this(context.fileTokens, context.fileSource, context.filePath)
    }

    override def toString: String = {
        val core = tokens.map(_._1).take(5).mkString(" ")
        if (tokens.length > 5) core + "..."
        else core
    }

    override def first: T = tokens.head._1

    override def rest: Reader[T] = new TokenReader(tokens.tail, source,  filePath)

    override def pos: Position = tokens.headOption.map(_._2).getOrElse(NoPosition)

    override def atEnd: Boolean = tokens.isEmpty
}