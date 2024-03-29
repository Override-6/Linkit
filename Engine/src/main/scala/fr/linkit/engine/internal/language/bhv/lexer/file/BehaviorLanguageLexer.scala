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

package fr.linkit.engine.internal.language.bhv.lexer.file

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.lexer.AbstractLexer
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageValues._
import fr.linkit.engine.internal.language.bhv.parser.ParserContext
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object BehaviorLanguageLexer extends AbstractLexer with RegexParsers {
    

    override type Token = BehaviorLanguageToken
    override protected val symbols  = BehaviorLanguageSymbol.values()
    override protected val keywords = BehaviorLanguageKeyword.values()
    
    override protected def symbolsRegex: Regex = BehaviorLanguageSymbol.RegexSymbols
    
    override def skipWhitespace: Boolean = true
    
    /////////// Parsers
    
    private val stringLiteral = pos("\"([^\"\\\\]|\\\\.)*\"".r ^^ (_.drop(1).dropRight(1)) ^^ Literal)
    private val identifier    = pos(identifierParser ^^ Identifier)
    private val numberParser  = pos("[0-9]+(\\.[0-9]+)?".r ^^ Number)
    private val boolParser    = pos("true|false".r ^^ Bool)
    
    implicit private def tokenToParser(token: Token): Parser[String] = literal(token.toString)
    
    //NOTE: order is important
    private val tokensParser = rep(keywordParser | symbolParser | boolParser | numberParser |
                                           stringLiteral | identifier)
    
    def tokenize(input: CharSequenceReader, filePath: String): ParserContext[Token] = {
        parseAll(tokensParser, input) match {
            case NoSuccess(msg, n)  =>
                throw new BHVLanguageException(makeErrorMessage(msg, "Failure", n.pos, n.source.toString, filePath))
            case Success(tokens, _) =>
                ParserContext(filePath, input.source.toString, tokens)
        }
    }
    
}
