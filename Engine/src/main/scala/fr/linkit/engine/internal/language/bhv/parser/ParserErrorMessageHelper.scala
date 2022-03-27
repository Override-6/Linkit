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

import scala.util.parsing.input.Position

object ParserErrorMessageHelper {

    def makeErrorMessage(msg: String, kind: String, pos: Position, fileSource: String, filePath: String): String = {
        val line  = fileSource
                .lines().toArray(new Array[String](_))
                .drop(pos.line - 1).headOption.getOrElse("")
        val start = pos.column
        var end   = line.indexWhere(_ == ' ', start)
        if (end == -1) end = line.length

        val identCount = line.takeWhile(_ == ' ').length
        val cursor     = " " * (start - 1 - identCount) + "^" * (end - start + 1)
        s"""
           |$kind at ${filePath.tail}:$pos: ${unescape(msg)}
           |${line.trim}
           |$cursor
           |""".stripMargin
    }

    def makeErrorMessage(msg: String, kind: String, reader: TokenReader[_]): String =
        makeErrorMessage(msg, kind, reader.pos, reader.source, reader.filePath)

    private val escapedChars = Map(escapeAll(
        "\\n", "\\f", "\\r", "\\b", "\\t",
    ): _*)

    private def escapeAll(strings: String*): Seq[(String, String)] = strings.map(s => (s.translateEscapes(), s))

    private def unescape(s: String): String = {
        escapedChars.foldLeft(s) { case (acc, (e, ue)) => acc.replace(e, ue) }
    }

}
