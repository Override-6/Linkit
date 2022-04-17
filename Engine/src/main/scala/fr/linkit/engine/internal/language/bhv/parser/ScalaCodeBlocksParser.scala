package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.ast.ScalaCodeBlock
import fr.linkit.engine.internal.language.bhv.integration.LambdaCallerClassBlueprint
import fr.linkit.engine.internal.language.bhv.lexer.code.ScalaCodeBlockSymbol._
import fr.linkit.engine.internal.language.bhv.lexer.code.ScalaCodeBlockValues.{Identifier, _}
import fr.linkit.engine.internal.language.bhv.lexer.code.{ScalaCodeBlockToken, ScalaCodeBlocksLexer}

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

object ScalaCodeBlocksParser extends Parsers {

    override type Elem = ScalaCodeBlockToken

    private val identifierParser = accept("identifier", { case Identifier(identifier) => identifier }).+ ^^ (_.mkString(" "))
    private val valueParser      = ValueOpen ~> At ~> identifierParser ~ (Colon ~> identifierParser).? <~ ValueClose ^^ {
        case name ~ types =>
            val tpe = types.getOrElse("scala.Any")
            (LambdaCallerClassBlueprint.getPropertyAccessCodeString(name, tpe), tpe)
    } withFailureMessage("external value access syntax is wrong. Syntax must be: £{@<value>: <valueType>}")
    private val fragmentParser   = accept("code fragment", { case CodeFragment(fragment) => fragment })

    def parse(input: CharSequenceReader): ScalaCodeBlock = {
        val tokens = ScalaCodeBlocksLexer.tokenize(input, "<scala block>")
        phrase(rep(fragmentParser | valueParser)).apply(new TokenReader(tokens)) match {
            case NoSuccess(msg, _) => throw new BHVLanguageException(s"Failure with scala block external access value: $msg")
            case Success(x, _)     =>
                val blocks = for (it <- x) yield {
                    it match {
                        case (propertyAccess: String, propertyTpe: String) => (propertyAccess, propertyTpe)
                        case blockPart: String                             => (blockPart, null)
                    }
                }
                ScalaCodeBlock(blocks.map(_._1).mkString("").dropRight(1), blocks.map(_._2).filter(_ != null).toArray)
        }
    }

}
