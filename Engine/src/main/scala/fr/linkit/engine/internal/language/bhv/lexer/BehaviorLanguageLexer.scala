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
        Modifier, Class, Synchronize).map(x => (x.toString, x)).toMap
    private final val symbolTokens = Seq(
        BracketLeft, BracketRight, SquareBracketLeft,
        SquareBracketRight, Arrow,
        Comma, ParenRight, ParenLeft
    ).map(x => (x.toString, x)).toMap

    /////////// Parsers
    private val stringToken     = "\\w+".r.filter(stringTokens.contains) ^^ stringTokens
    private val symbolToken     = SymbolsRegex.filter(symbolTokens.contains) ^^ symbolTokens
    private val codeBlock       = "${" ~> codeBlockParser ^^ CodeBlock
    private val stringLiteral   = "\"([^\"\\\\]|\\\\.)*\"".r ^^ (_.drop(1).dropRight(1)) ^^ Literal
    private val identifier      = "[\\w.$£]+".r ^^ Identifier

    private def codeBlockParser: Parser[String] = {
        var bracketDepth = 1

        def code: Parser[String] = {
            skipWhiteSpace = false
            ("[}{]|([\\s\\S]+?[}{])".r ^^ { s =>
                if (s.last == '{') bracketDepth += 1 else bracketDepth -= 1
                s
            }) ~! (if (bracketDepth == 0) "" else code) ^^ {
                case a ~ b =>
                    skipWhiteSpace = true
                    bracketDepth = 1
                    a + b
            }
        }

        code
    }

    implicit private def tokenToParser(token: Token): Parser[String] = literal(token.toString)

    def tokenize(input: CharSequenceReader): List[Token] = {
        val tokens = stringToken | symbolToken |
                codeBlock | stringLiteral | identifier
        parseAll(rep(tokens), input) match {
            case NoSuccess(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
            case Success(r, _)     => r
        }
    }
}
