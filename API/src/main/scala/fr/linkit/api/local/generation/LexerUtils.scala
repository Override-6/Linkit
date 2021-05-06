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

package fr.linkit.api.local.generation

import java.util.regex.Pattern
import scala.collection.mutable.ListBuffer

object LexerUtils {

    def positions(str: String, blueprint: String): Seq[Int] = {
        var lastIndex = blueprint.indexOf(str)
        val locations = ListBuffer.empty[Int]
        while (lastIndex != -1) {
            locations += lastIndex
            lastIndex = blueprint.indexOf(str, lastIndex + 1)
        }
        locations.toSeq
    }

    def positionsBetween(quotesRegex: String, blueprint: String): Seq[(String, Int)] = {
        val matcher      = Pattern.compile(s"$quotesRegex(.*)$quotesRegex").matcher(blueprint)
        val buffer = ListBuffer.empty[(String, Int)]
        while (matcher.find()) {
            buffer += ((matcher.group(1), matcher.start()))
        }
        buffer.toSeq
    }

    def nextBlock(blueprint: String, pos: Int): String = {
        var blockDepth     = 0
        val semiBlock      = blueprint.drop(pos + 1) //remove the first '{'
        var lastChar: Char = blueprint(pos)
        var isInString     = false
        for (i <- semiBlock.indices) {
            semiBlock(i) match {
                case '\"' if lastChar != '\\'              => isInString = !isInString
                case '{' if !isInString                    => blockDepth += 1
                case '}' if blockDepth == 0 && !isInString =>
                    return semiBlock.take(i)
                case '}' if !isInString                    => blockDepth -= 1
                case _                                     =>
            }
            lastChar = semiBlock(i)
        }
        semiBlock
    }

}
