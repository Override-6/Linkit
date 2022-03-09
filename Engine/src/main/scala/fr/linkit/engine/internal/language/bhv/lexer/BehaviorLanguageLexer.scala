package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageTokens._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object BehaviorLanguageLexer extends AbstractLexer with RegexParsers {

    private var skipWhiteSpace = true

    override protected val symbolsRegex: Regex = SymbolsRegex

    override def skipWhitespace: Boolean = skipWhiteSpace

    /////////// Parsers

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
        val tokens = stringTokenParser | symbolTokenParser |
                codeBlock | stringLiteral | identifier
        parseAll(rep(tokens), input) match {
            case NoSuccess(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
            case Success(r, _)     => r
        }
    }
}
