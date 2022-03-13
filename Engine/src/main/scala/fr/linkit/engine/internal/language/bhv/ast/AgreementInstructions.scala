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

package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.cache.sync.contract.behavior.EngineTag

trait AgreementInstruction

case object DiscardAll extends AgreementInstruction

case object AcceptAll extends AgreementInstruction

case class Accept(tags: Seq[EngineTag]) extends AgreementInstruction

case class Discard(tags: Seq[EngineTag]) extends AgreementInstruction

case class Appoint(appointed: EngineTag) extends AgreementInstruction

case class Is(a: EngineTag, b: EngineTag, reverse: Boolean)

object Is {

    implicit class IsBuilder(a: EngineTag) {

        def is(b: EngineTag) = Is(a, b, false)
        def is_not(b: EngineTag) = Is(a, b, true)
nh    }
}

case class If(is: Is, ifTrue: Seq[AgreementInstruction], ifFalse: Seq[AgreementInstruction]) extends AgreementInstruction