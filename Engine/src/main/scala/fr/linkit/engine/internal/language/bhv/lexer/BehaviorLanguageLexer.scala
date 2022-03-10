package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageTokens._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object BehaviorLanguageLexer extends AbstractLexer with RegexParsers {

    private var skipWhiteSpace = true

    override type Token = BehaviorLanguageTokens.Token

    override protected def symbolsRegex: Regex = SymbolsRegex

    override def skipWhitespace: Boolean = skipWhiteSpace

    override protected def keywords = Seq(
        Import, Describe, Scala,
        Enable, Disable, Hide, As, Method,
        Modifier, Class, Synchronize)
    override protected def symbols  = Seq(
        BracketLeft, BracketRight, SquareBracketLeft,
        SquareBracketRight, Arrow, Not,
        Comma, ParenRight, ParenLeft
    )

    /////////// Parsers

    private val codeBlock     = "${" ~> codeBlockParser ^^ CodeBlock
    private val stringLiteral = "\"([^\"\\\\]|\\\\.)*\"".r ^^ (_.drop(1).dropRight(1)) ^^ Literal
    private val identifier    = "[\\S]+".r ^^ Identifier

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
        val tokens = stringParser | symbolParser |
                codeBlock | stringLiteral | identifier
        parseAll(rep(tokens), input) match {
            case NoSuccess(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
            case Success(r, _)     => r
        }
    }
}
