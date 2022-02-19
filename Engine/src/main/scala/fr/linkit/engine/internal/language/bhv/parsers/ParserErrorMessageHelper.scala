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

package fr.linkit.engine.internal.language.bhv.parsers

import scala.util.parsing.input.{CharSequenceReader, Position}

object ParserErrorMessageHelper {

    def makeErrorMessage(msg: String, kind: String, reader: CharSequenceReader, pos: Position): String = {
        val line   = reader.source
                .toString
                .lines().toArray(new Array[String](_))
                .drop(pos.line - 1).head
        val start  = pos.column
        var end    = line.indexWhere(_ == ' ', start)
        if (end == -1) end = line.length

        val identCount = line.takeWhile(_==' ').length
        val cursor = " " * (start - 1 - identCount) + "^" * (end - start + 1)
        s"""
           |$kind at $pos: $msg
           |${line.trim}
           |$cursor
           |""".stripMargin
    }

}
