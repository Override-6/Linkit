package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.lexer.ScalaCodeBlocksTokens._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object ScalaCodeBlocksLexer extends AbstractLexer with RegexParsers {
    override protected def symbolsRegex: Regex = SymbolsRegex

    private val identifier = "[\\S]+".r ^^ Identifier

    def tokenize(input: CharSequenceReader): List[Token] = {
        parseAll(rep(symbolTokenParser | identifier), input) match {
            case NoSuccess(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
            case Success(x, _)     => x.foldRight(List[Token]()) { case (token, hd) =>
                (token, hd.lastOption) match {
                    case (Identifier(id), fragment) => fragment.fold[Token](CodeFragment(id)) {
                        case CodeFragment(last) => CodeFragment(last + ' ' + id)
                        case x                  => x
                    } :: hd
                    case (NewLine, fragment)        => fragment.fold[Token](CodeFragment("\n")) {
                        case CodeFragment(last) => CodeFragment(last + "\n")
                        case x                  => x
                    } :: hd
                    case (t: Token, _) => t :: hd
                }
            }
        }
    }
}
