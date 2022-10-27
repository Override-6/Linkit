/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.api.gnom.network.{EngineTag, IdentifierTag}
import fr.linkit.engine.internal.language.bhv.ast.Equals.IsBuilder
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageKeyword._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageSymbol._

import scala.annotation.switch
import scala.util.chaining.scalaUtilChainingOps

object AgreementBuilderParser extends BehaviorLanguageParser {

    private val tag = identifier ^^ getTag

    private val instruction = {
        val discardAll = Discard ~ Star ^^^ DiscardAll
        val acceptAll  = Accept ~ Star ^^^ AcceptAll
        val accept     = Accept ~> rep1sep(tag, And) ^^ AcceptEngines
        val discard    = Discard ~> rep1sep(tag, And) ^^ DiscardEngines
        val appoint    = Appoint ~> tag ^^ AppointEngine
        discard | discardAll | acceptAll | accept | appoint
    }

    private val ifInstruction = {
        val test                   = (tag <~ Is) ~ Not.? ~ tag ^^ {
            case a ~ None ~ b    => a is b
            case a ~ Some(_) ~ b => a isNot b
        }
        val contextualInstructions = block(repsep(instruction, Arrow))
        (If ~ ParenLeft ~> test <~ ParenRight) ~ contextualInstructions ~ ((Else ~> contextualInstructions) | success(List())) ^^ {
            case test ~ ifTrue ~ ifFalse => Condition(test, ifTrue, ifFalse)
        }
    }

    private def block[P](parser: Parser[P]): Parser[P] = BracketLeft ~> parser <~ BracketRight

    private val instructions = repsep(instruction | ifInstruction, Arrow).pipe(p => block(p) | p)

    private val agreementParser = {

        Agreement ~> (identifier <~ Equal) ~ instructions ^^ {
            case name ~ instructions => AgreementBuilder(Some(name), instructions)
        }
    }

    private[parser] val anonymous  : Parser[AgreementBuilder] = instructions ^^ (AgreementBuilder(None, _))
    private[parser] val declaration: Parser[AgreementBuilder] = agreementParser

    private implicit def toToken(token: Elem): Parser[Elem] = accept(token)

    def getTag(name: String): EngineTag = (name: @switch) match {
        case "owner"       => EngineTags.OwnerEngine
        case "root_owner"  => EngineTags.RootOwnerEngine
        case "cache_owner" => EngineTags.CacheOwnerEngine
        case "current"     => EngineTags.CurrentEngine
        case _             => IdentifierTag(name)
    }

}