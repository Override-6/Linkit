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

import fr.linkit.engine.internal.language.bhv.lexer
import fr.linkit.engine.internal.language.bhv.lexer.file.{BehaviorLanguageKeyword, BehaviorLanguageSymbol}

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.Position

abstract class AbstractLexer extends RegexParsers {
    
    protected def whitespaceChar = "\\s".r
    
    override final protected val whiteSpace: Regex = {
        val ws = whitespaceChar.regex
        val r  = (ws + "*(((//.*\\n?)|(/\\*[\\s\\S]+?\\*/))" + ws + "*)|" + ws + "+").r
        r
    }
    
    type Token <: lexer.Token
    type PosToken = (Token, Position)
    
    protected val symbols : Array[_ <: Token with Symbol]
    protected val keywords: Array[_ <: Token with Keyword]
    
    protected def symbolsRegex: Regex
    
    protected lazy val keywordParser   : Parser[PosToken] = {
        val keywords = this.keywords.map(x => (x.value, x)).toMap
        pos("[\\w`]+".r.filter(s => s.head != '`' && s.last != '`')
                    .filter(keywords.contains) ^^ keywords.apply)
    }
    protected lazy val symbolParser    : Parser[PosToken] = {
        val symbols = this.symbols.map(x => (x.value, x)).toMap
        pos(symbolsRegex ^^ symbols)
    }
    protected lazy val identifierParser: Parser[String]   = {
        s"\\w+".r
    }
    
    override protected def handleWhiteSpace(source: CharSequence, offset: Int): Int = {
        if (skipWhitespace) {
            whiteSpace.findPrefixMatchOf(new SubSequence(source, offset)) match {
                case Some(matched) =>
                    handleWhiteSpace(source, offset + matched.end)
                case None          =>
                    offset
            }
        } else offset
    }
    
    implicit def pos[P <: Token](parser: Parser[P]): Parser[(P, Position)] = {
        Parser { in =>
            parser(in) match {
                case Failure(msg, in) => Failure(msg, in)
                case Error(msg, in)   => Error(msg, in)
                case Success(x, next) => Success((x, in.pos), next)
            }
        }
    }
}
