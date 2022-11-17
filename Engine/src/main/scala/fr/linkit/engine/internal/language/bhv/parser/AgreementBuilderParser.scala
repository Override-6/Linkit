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

import fr.linkit.api.gnom.cache.sync.contract.OwnerEngine
import fr.linkit.api.gnom.network.tag.{Current, EngineTag, Group, IdentifierTag, NameTag, Nobody, Server}
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
        val accept     = Accept ~> rep1sep(tag, Comma) ^^ AcceptEngines
        val discard    = Discard ~> rep1sep(tag, Comma) ^^ DiscardEngines
        val appoint    = Appoint ~> tag ^^ AppointEngine
        discard | discardAll | acceptAll | accept | appoint
    }

    private def block[P](parser: Parser[P]): Parser[P] = BracketLeft ~> parser <~ BracketRight

    private val instructions = rep1sep(instruction, Arrow).pipe(p => block(p) | p)

    private val agreementParser = {
        Agreement ~> (identifier <~ Equal) ~ instructions ^^ {
            case name ~ instructions => AgreementBuilder(Some(name), instructions)
        }
    }

    private[parser] val anonymous  : Parser[AgreementBuilder] = instructions ^^ (AgreementBuilder(None, _))
    private[parser] val declaration: Parser[AgreementBuilder] = agreementParser

    private implicit def toToken(token: Elem): Parser[Elem] = accept(token)

    def getTag(name: String): EngineTag = {
        name.toLowerCase match {
            case "server"  => Server
            case "current" => Current
            case "owner"   => OwnerEngine
            case "nobody"  => Nobody
            case s"#$id"   => IdentifierTag(id)
            case s"@$gp"   => Group(gp)
            case s"~$name" => NameTag(name)
        }
    }

}
