package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.lexer.ScalaCodeBlocksTokens._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object ScalaCodeBlocksLexer extends AbstractLexer with RegexParsers {

    override protected def symbolsRegex: Regex = SymbolsRegex

    override type Token = ScalaCodeBlocksTokens.Token

    override protected val whiteSpace: Regex = "[ \t\r]+".r

    override protected def keywords: Seq[Token] = Seq()

    override protected def symbols: Seq[Token] = Seq(ValueOpen, ValueClose, DoublePoints)

    private val identifier   = "[^£:\\s}]+".r ^^ Identifier
    private val codeFragment = ("[\\s\\S]+?(?=£\\{)".r | "[\\s\\S]+".r) ^^ CodeFragment
    private val value        = rep1("£{" ~> rep((symbolParser | identifier) - "}")) ~ symbolParser ^^ {
        case a ~ b => ValueOpen :: (a :+ b) }
    private val parser       = rep(value | codeFragment)

    def tokenize(input: CharSequenceReader): List[Token] = {
        parseAll(parser, input) match {
            case NoSuccess(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
            case Success(x, _)     => x.foldLeft(List[Token]()) {
                case (hd, token: Token)                                               => hd :+ token
                case (hd, ::(head: Token, ::(next: List[Token], (last: Token) :: _))) =>
                    (hd :+ head) ++ next :+ last
            }
        }
    }
}
