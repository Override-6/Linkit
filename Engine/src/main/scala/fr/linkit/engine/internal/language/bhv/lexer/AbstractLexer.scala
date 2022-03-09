package fr.linkit.engine.internal.language.bhv.lexer

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

abstract class AbstractLexer extends RegexParsers {

    type Token <: AbstractTokens#Token

    protected def keywords: Seq[Token]
    protected def symbols : Seq[Token]

    protected def symbolsRegex: Regex

    protected val stringParser: Parser[Token] = {
        val keywords = this.symbols.map(x => (x.toString, x)).toMap
        "\\w+".r.filter(keywords.contains) ^^ keywords
    }
    protected val symbolParser: Parser[Token] = {
        val symbols = this.symbols.map(x => (x.toString, x)).toMap
        symbolsRegex.filter(symbols.contains) ^^ symbols
    }

}
