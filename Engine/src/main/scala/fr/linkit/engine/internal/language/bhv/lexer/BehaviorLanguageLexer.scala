package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageTokens._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object BehaviorLanguageLexer extends RegexParsers {
    private var skipWhiteSpace = true

    override def skipWhitespace: Boolean = skipWhiteSpace

    private final val stringTokens = Seq(
        Import, Describe, Scala,
        Enable, Disable, Hide, As, Method,
        Modifier, Class).map(x => (x.toString, x)).toMap
    private final val symbolTokens = Seq(
        BracketLeft, BracketRight, SquareBracketLeft,
        SquareBracketRight, Arrow
    ).map(x => (x.toString, x)).toMap

    private val stringTokenParser = "\\w+".r.filter(stringTokens.contains) ^^ stringTokens
    private val symbolTokenParser = SymbolRegexMatch.filter(symbolTokens.contains) ^^ symbolTokens
    private val scalaBlockParser  = "${" ~> codeBlock ^^ ScalaBlock
    private val stringLiteralParser = "\"([^\"\\\\]|\\\\.)*\"".r ^^ (_.drop(1).dropRight(1)) ^^ Literal
    private val identifierParser  = "\\S+".r ^^ Identifier

    private def codeBlock: Parser[String] = {
        var bracketDepth = 1

        def code: Parser[String] = {
            skipWhiteSpace = false
            ("[\\s\\S]+?[}{]".r ^^ { s =>
                if (s.last == '{') bracketDepth += 1 else bracketDepth -= 1
                skipWhiteSpace = true
                s
            }) ~ (if (bracketDepth == 0) "" else code) ^^ { case a ~ b => a + b }
        }

        code
    }

    private val tokenParser = {
        stringTokenParser | symbolTokenParser |
            scalaBlockParser | stringLiteralParser | identifierParser
    }

    implicit private def tokenToParser(token: Token): Parser[String] = literal(token.toString)

    def tokenize(input: CharSequenceReader): List[Token] = parseAll(rep(tokenParser), input) match {
        case NoSuccess(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
        case Success(r, _)     =>
            r
    }
}
