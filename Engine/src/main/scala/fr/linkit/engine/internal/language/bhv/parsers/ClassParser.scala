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

import fr.linkit.engine.internal.language.bhv.results.FileImports

import scala.util.parsing.combinator.RegexParsers

class ClassParser(imports: FileImports) extends RegexParsers {

    override val whiteSpace = "[ \t\r\f]+".r

    override def skipWhitespace = true

    private val head = "describe" ~ ("static class" | "class") ~
            ("[^\\s]+".r ^^ imports.find) ~ "{"
    private val methodSignature = "(\\w+\\()([\\w,.\\s\\[\\]]+)\\)".r
    private val disableMethod = "(disable)|(disable method)".r ~ methodSignature
    private val enableMethod = "(enable)|(enable method)".r ~ methodSignature ~ "{"


}
