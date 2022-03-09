package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageTokens.{Arrow, As, BracketLeft, BracketRight, Class, Comma, Describe, Disable, Enable, Hide, Import, Method, Modifier, ParenLeft, ParenRight, Scala, SquareBracketLeft, SquareBracketRight, SymbolsRegex, Synchronize}

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

abstract class AbstractLexer extends RegexParsers {

    protected final val stringTokens = Seq(
        Import, Describe, Scala,
        Enable, Disable, Hide, As, Method,
        Modifier, Class, Synchronize).map(x => (x.toString, x)).toMap
    protected final val symbolTokens = Seq(
        BracketLeft, BracketRight, SquareBracketLeft,
        SquareBracketRight, Arrow,
        Comma, ParenRight, ParenLeft
    ).map(x => (x.toString, x)).toMap

    protected def symbolsRegex: Regex

    protected val stringTokenParser = "\\w+".r.filter(stringTokens.contains) ^^ stringTokens
    protected val symbolTokenParser = symbolsRegex.filter(symbolTokens.contains) ^^ symbolTokens

}
