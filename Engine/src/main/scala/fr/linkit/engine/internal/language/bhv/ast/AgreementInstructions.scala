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

package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.network.tag.EngineTag

trait AgreementInstruction

case object DiscardAll extends AgreementInstruction

case object AcceptAll extends AgreementInstruction

case class AcceptEngines(tags: List[EngineTag]) extends AgreementInstruction

case class DiscardEngines(tags: List[EngineTag]) extends AgreementInstruction

case class AppointEngine(appointed: EngineTag) extends AgreementInstruction


sealed trait AgreementProvider extends Product

case class AgreementBuilder(agreementName: Option[String], instructions: List[AgreementInstruction]) extends AgreementProvider

case class AgreementReference(name: String) extends AgreementProvider
