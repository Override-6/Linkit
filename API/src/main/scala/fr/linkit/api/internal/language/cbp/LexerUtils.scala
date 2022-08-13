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

package fr.linkit.api.internal.language.cbp

import java.util.regex.{MatchResult, Pattern}
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

    def expressionsBetween(regexA: String, regexB: String, blueprint: String): Seq[(String, Int)] = {
        val matcher = Pattern.compile(s"$regexA(.*?)$regexB").matcher(blueprint)
        matcher.results().toArray(new Array[MatchResult](_)).map { result =>
            (result.group(1).trim, result.start())
        }
    }

    def nextBlock(blueprint: String, pos: Int): String = {
        var blockDepth     = 0
        val start          = blueprint.indexOf('{', pos)
        val semiBlock      = blueprint.drop(start + 1) //remove the first '{'
        var lastChar: Char = blueprint(start)
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
