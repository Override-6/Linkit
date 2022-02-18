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

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.compilation.ScalaCodeRepository

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object MethodContentParser extends RegexParsers {

    private val modify      = "modify" ~ rep("\\d+".r ^^ (_.toInt) | "&") ~ "{" ~
            input("remote_to_current", DistantToCurrent) |
            input("current_to_remote", CurrentToDistant) ~
                    "}"
    private val returnvalue = "synchronized returnvalue"

    private def input(name: String, f: String => LambdaExpression): Parser[LambdaExpression] = {
        name ~ "->" ~> ("\\$\\{.*}".r ^^ f)
    }

    private sealed trait LambdaExpression

    private case class DistantToCurrent(expression: String) extends LambdaExpression
    private case class CurrentToDistant(expression: String) extends LambdaExpression

    def parseContent(input: CharSequenceReader, repo: ScalaCodeRepository): Unit = {
        parse(repNM(0, 1, modify), input) match {
            case Failure(msg, _) => throw new BHVLanguageException(s"Failure: ${msg}")
            case Error(msg, _)   => throw new BHVLanguageException(s"Error: ${msg}")
            case Success()
        }
    }

}