package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.compilation.LambdaRepositoryClassBlueprint
import fr.linkit.engine.internal.language.bhv.lexer.ScalaCodeBlocksTokens._
import fr.linkit.engine.internal.language.bhv.lexer.{ScalaCodeBlocksLexer, ScalaCodeBlocksTokens}
import fr.linkit.engine.internal.language.bhv.parser.ASTTokens.ScalaCodeBlock
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{CharSequenceReader, NoPosition, Position, Reader}

object ScalaCodeBlocksParser extends Parsers {

    override type Elem = Token

    private val identifierParser = accept("identifier", { case Identifier(identifier) => identifier })
    private val valueParser      = ValueOpen ~> identifierParser ~ (DoublePoints ~> identifierParser).? <~ BracketRight ^^ {
        case name ~ tpe => LambdaRepositoryClassBlueprint.getPropertyAccessCodeString(name, tpe.getOrElse("scala.Any"))
    }
    private val fragmentParser   = accept("code fragment", { case CodeFragment(fragment) => fragment })

    def parse(input: CharSequenceReader): ScalaCodeBlock = {
        val tokens = ScalaCodeBlocksLexer.tokenize(input)
        (fragmentParser | valueParser).apply(new TokenReader(tokens)) match {
            case NoSuccess(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
            case Success(x, _)     =>
                ScalaCodeBlock(x.mkString(""))
        }
    }

    class TokenReader(tokens: Seq[Token]) extends Reader[Token] {
        override def first: ScalaCodeBlocksTokens.Token = tokens.head

        override def rest: Reader[ScalaCodeBlocksTokens.Token] = new TokenReader(tokens.tail)

        override def pos: Position = NoPosition

        override def atEnd: Boolean = tokens.isEmpty
    }

}
