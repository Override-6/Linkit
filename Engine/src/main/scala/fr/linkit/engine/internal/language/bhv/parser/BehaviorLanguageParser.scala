/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.parser

import scala.util.parsing.combinator.Parsers
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageTokens._
import fr.linkit.engine.internal.language.bhv.parser.ASTTokens._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

class BehaviorLanguageParser extends Parsers {

    override type Elem = Token

    private val modifiers = rep(
        scalaCodeIntegration("distant_to_current", RemoteToCurrentModifier)
                | scalaCodeIntegration("current_to_distant", CurrentToRemoteModifier)
                | scalaCodeIntegration("current_to_distant_event", CurrentToRemoteEvent)
                | scalaCodeIntegration("distant_to_current_event", RemoteToCurrentEvent)
    )

    private val identifierParser      = accept("identifier", { case Identifier(identifier) => identifier })
    private val literalParser         = accept("literal", { case Literal(str) => str })
    private val codeBlockParser       = accept("scala code", { case CodeBlock(code) => toScalaCodeToken(code) })
    private val numberParser          = identifierParser ^^ (_.toInt)
    private val methodSignatureParser = {
        val param = Synchronize.? ~ identifierParser ^^ { case s ~ id => MethodParam(s.isDefined, id) }

        def params: Parser[List[MethodParam]] = {
            (param ~ Comma ~ params) ^^ { case param ~ _ ~ hd => param :: hd } | param ^^ (List(_))
        }

        identifierParser ~ ParenLeft ~ params ~ ParenRight ^^ { case name ~ _ ~ params ~ _ => MethodSignature(name, params) }
    }
    private val methodModifierParser  = {
        val param = numberParser | (ReturnValue ^^^ -1)

        def params: Parser[List[Int]] = {
            (param ~ And ~ params) ^^ { case num ~ _ ~ hd => num :: hd } | param ^^ (List(_))
        }

        Modifier ~> params ~ BracketLeft ~ modifiers <~ BracketRight ^^ {
            case concernedComps ~ _ ~ lambdas => MethodComponentsModifier(
                concernedComps.filter(_ != ReturnValue).map((_, lambdas)).toMap,
                if (concernedComps.contains(ReturnValue)) lambdas else Seq())
        }
    }

    private def scalaCodeIntegration(name: String, kind: LambdaKind): Parser[LambdaExpression] = {
        Identifier(name) ~ Arrow ~> codeBlockParser ^^ (LambdaExpression(_, kind))
    }

    private def reformatCode(s: String): String = {
        val firstLineIndex = s.indexOf('\n')
        (s.take(firstLineIndex).stripLeading() + s.drop(firstLineIndex).stripIndent()).dropRight(1)
    }

    private def toScalaCodeToken(code: String): ScalaCodeBlock = {
        val formattedCode = reformatCode(code)
        var pos           = 0

        val externalReference = "[\\s\\S]+?£\\{".r ~> ".[\\w/]+".r ~ (":" ~> "[^}]+".r).? <~ "}" ^^ {
            case name ~ clazz =>
                pos = formattedCode.indexOf("£{", pos + 1)
                ScalaCodeExternalObject(name, clazz.getOrElse("scala.Any"), pos)
        }
        val refs              = parse(rep(externalReference), code) match {
            case Success(x, _)     =>
                x
            case NoSuccess(msg, n) =>
                val errorMsg = makeErrorMessage(msg, "Failure into scala block code", new CharSequenceReader(formattedCode), n.pos)
                throw new BHVLanguageException(errorMsg)
        }
        ScalaCodeBlock(formattedCode, refs)
    }

}
