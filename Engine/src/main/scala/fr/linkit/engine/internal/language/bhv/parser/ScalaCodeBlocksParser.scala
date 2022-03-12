package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.ast.ScalaCodeBlock
import fr.linkit.engine.internal.language.bhv.compilation.LambdaRepositoryClassBlueprint
import fr.linkit.engine.internal.language.bhv.lexer.ScalaCodeBlockSymbol._
import fr.linkit.engine.internal.language.bhv.lexer.ScalaCodeBlockValues._
import fr.linkit.engine.internal.language.bhv.lexer.{ScalaCodeBlockToken, ScalaCodeBlocksLexer}

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{CharSequenceReader, NoPosition, Position, Reader}

object ScalaCodeBlocksParser extends Parsers {

    override type Elem = ScalaCodeBlockToken

    private val identifierParser = accept("identifier", { case Identifier(identifier) => identifier }).+ ^^ (_.mkString(" "))
    private val valueParser      = ValueOpen ~> identifierParser ~ (Colon ~> identifierParser).? <~ ValueClose ^^ {
        case name ~ types =>
            LambdaRepositoryClassBlueprint.getPropertyAccessCodeString(name, types.getOrElse("scala.Any"))
    }
    private val fragmentParser   = accept("code fragment", { case CodeFragment(fragment) => fragment })

    /*private def parser: Parser[List[String]] = {
        val p = fragmentParser | valueParser
        p ~ parser ^^ {case s ~ hd => s :: hd} | p ^^ (List(_))
    }*/

    def parse(input: CharSequenceReader): ScalaCodeBlock = {
        val tokens = ScalaCodeBlocksLexer.tokenize(input)
        phrase(rep(fragmentParser | valueParser)).apply(new TokenReader(tokens)) match {
            case NoSuccess(msg, _) => throw new BHVLanguageException(s"Failure with scala block external access value: $msg")
            case Success(x, _)     =>
                ScalaCodeBlock(x.mkString(""))
        }
    }

    class TokenReader(tokens: Seq[Elem]) extends Reader[Elem] {

        override def first: Elem = tokens.head

        override def rest: Reader[Elem] = new TokenReader(tokens.tail)

        override def pos: Position = NoPosition

        override def atEnd: Boolean = tokens.isEmpty
    }

}
