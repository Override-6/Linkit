package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.lexer.ScalaCodeBlockSymbol._
import fr.linkit.engine.internal.language.bhv.lexer.ScalaCodeBlockValues._
import fr.linkit.engine.internal.language.bhv.parser.ParserContext
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{CharSequenceReader, NoPosition}

object ScalaCodeBlocksLexer extends AbstractLexer with RegexParsers {

    override type Token = ScalaCodeBlockToken
    override protected val symbols  = ScalaCodeBlockSymbol.values()
    override protected val keywords = Array[Token with Keyword]()

    override protected def symbolsRegex: Regex = ScalaCodeBlockSymbol.RegexSymbols

    override protected def whitespaceChar: Regex = "[ \t\r]".r

    private val identifier   = pos("[^£:\\s}]+".r ^^ Identifier)
    private val codeFragment = pos(("[\\s\\S]+?(?=£\\{)".r | "[\\s\\S]+".r) ^^ CodeFragment)
    private val value        = rep1("£{" ~> rep((symbolParser | identifier) - "}") ~ symbolParser ^^ {
        case a ~ b => (ValueOpen, a.headOption.map(_._2).getOrElse(NoPosition)) :: (a :+ b)
    })
    private val parser       = rep(value | codeFragment)

    def tokenize(input: CharSequenceReader, filePath: String): ParserContext[Token] = {
        parseAll(parser, input) match {
            case NoSuccess(msg, n) =>
                throw new BHVLanguageException(makeErrorMessage(msg, "Failure", n.pos, input.source.toString, filePath))
            case Success(x, _)     =>
                val list = x.foldLeft(List[PosToken]()) {
                    case (hd, token: PosToken)            => hd :+ token
                    case (hd, List(list: List[PosToken])) => hd ++ list
                }
                ParserContext(filePath, input.source.toString, list)
        }
    }
}