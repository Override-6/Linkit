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

import fr.linkit.engine.internal.language.bhv.{P, Tokens}

import scala.util.parsing.combinator.RegexParsers

object ClassParser extends RegexParsers {

    override val whiteSpace = "[ \t\r\f]+".r
    override def skipWhitespace = true

    import fr.linkit.engine.internal.language.bhv.Tokens._
    val methodBody: P[_] = modify ~ rep1(number | and)
    val methodParser: P[_] = method ~ signature ~ (bracketOpen | Tokens.literal)

}
