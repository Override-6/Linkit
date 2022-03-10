package fr.linkit.engine.internal.language.bhv.lexer

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

abstract class AbstractLexer extends RegexParsers {

    type Tokens <: AbstractTokens
    final type Token = Tokens#Token

    protected val tokens: Tokens

    private lazy val symbols : Seq[Token] = tokens.symbols
    private lazy val keywords: Seq[Token] = tokens.keywords

    private val symbolsRegex: Regex = tokens.SymbolsRegex

    protected val keywordParser: Parser[Token] = {
        val keywords = this.keywords.map(x => (x.representation, x)).toMap
        "\\w+".r.filter(keywords.contains(_)) ^^ (keywords.apply(_))
    }
    protected val symbolParser : Parser[Token] = {
        val symbols = this.symbols.map(x => (x.representation, x)).toMap
        symbolsRegex.filter(symbols.contains) ^^ symbols
    }

}
