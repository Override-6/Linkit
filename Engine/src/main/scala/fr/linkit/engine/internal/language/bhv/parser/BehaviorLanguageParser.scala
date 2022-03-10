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

import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageTokens._
import fr.linkit.engine.internal.language.bhv.parser.ASTTokens._

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

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
    private val syncOrNot             = (Not.? <~ Synchronize ^^ (_.isEmpty)).? ^^ (s => SynchronizeState(s.isDefined, s.getOrElse(false)))
    private val methodSignatureParser = {
        val param = syncOrNot ~ identifierParser ^^ { case s ~ id => MethodParam(s, id) }
        val params = repsep(param, Comma)

        (identifierParser <~ ParenLeft) ~ params <~ ParenRight ^^ { case name ~ params => MethodSignature(name, params) }
    }
    private val methodModifierParser  = {
        val param  = ReturnValue | accept("identifier", { case x: Identifier => x })
        val params = repsep(param, And)

        Modifier ~> params ~ BracketLeft ~ modifiers <~ BracketRight ^^ {
            case concernedComps ~ _ ~ lambdas => MethodComponentsModifier(
                concernedComps.filterNot(_ == ReturnValue).map { case Identifier(num) => (num.toInt, lambdas) }.toMap,
                if (concernedComps.contains(ReturnValue)) lambdas else Seq())
        }
    }
    private val enabledMethodCore     = {
        (BracketLeft ~> rep(methodModifierParser) ~ (syncOrNot <~ ReturnValue).? <~ BracketRight).? ^^ (x => new ~(x.getOrElse(List(), SynchronizedState())))
    }
    private val enableMethodParser    = {
        (Enable ~> Method ~> methodSignatureParser) ~ (As ~> identifierParser).? ~ enabledMethodCore ^^ {
            case signature ~ referent ~ modifiers ~ syncRv =>
        }
    }

    private def scalaCodeIntegration(name: String, kind: LambdaKind): Parser[LambdaExpression] = {
        Identifier(name) ~ Arrow ~> codeBlockParser ^^ (LambdaExpression(_, kind))
    }

    private def toScalaCodeToken(sc: String): ScalaCodeBlock = {
        ScalaCodeBlocksParser.parse(new CharSequenceReader(sc))
    }
}
