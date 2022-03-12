package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.lexer

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

abstract class AbstractLexer extends RegexParsers {

    type Token <: lexer.Token

    protected val symbols : Array[_ <: Token with Symbol]
    protected val keywords: Array[_ <: Token with Keyword]

    protected def symbolsRegex: Regex

    protected lazy val keywordParser: Parser[Token] = {
        val keywords = this.keywords.map(x => (x.representation, x)).toMap
        "\\w+".r.filter(keywords.contains) ^^ keywords.apply
    }
    protected lazy val symbolParser : Parser[Token] = {
        val symbols = this.symbols.map(x => (x.representation, x)).toMap
        symbolsRegex.filter(symbols.contains) ^^ symbols
    }

}
