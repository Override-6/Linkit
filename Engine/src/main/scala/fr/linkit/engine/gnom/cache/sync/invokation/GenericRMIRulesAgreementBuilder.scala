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

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.EngineTags._
import fr.linkit.api.gnom.cache.sync.contract.behavior._
import fr.linkit.engine.gnom.cache.sync.invokation.GenericRMIRulesAgreementBuilder._


class GenericRMIRulesAgreementBuilder private(discarded: Seq[EngineTag], accepted: Seq[EngineTag],
                                              conditions: Seq[AgreementConditionResult],
                                              acceptAllTargets: Boolean,
                                              desiredEngineReturn: EngineTag) extends RMIRulesAgreementBuilder {

    def this() {
        this(Seq.empty, Seq.empty, Seq.empty, true, CurrentEngine)
    }

    def this(other: GenericRMIRulesAgreementBuilder) {
        this(other.discarded, other.accepted, other.conditions, other.acceptAllTargets, other.desiredEngineReturn)
    }


    override def acceptRootOwner(): this.type = accept(RootOwnerEngine)

    override def discardRootOwner(): this.type = discard(RootOwnerEngine)

    override def desireCurrentEngineToReturn(): this.type = {
        new GenericRMIRulesAgreementBuilder(discarded, accepted, conditions, acceptAllTargets, CurrentEngine)
    }

    override def desireRootOwnerEngineToReturn(): this.type = {
        new GenericRMIRulesAgreementBuilder(discarded, accepted, conditions, acceptAllTargets, RootOwnerEngine)
    }

    override def desireCacheOwnerEngineToReturn(): this.type = {
        new GenericRMIRulesAgreementBuilder(discarded, accepted, conditions, acceptAllTargets, CacheOwnerEngine)
    }

    private def discard(target: EngineTag): this.type = {
        new GenericRMIRulesAgreementBuilder(discarded :+ target, accepted.filterNot(_ == target), conditions, acceptAllTargets, desiredEngineReturn)
    }

    override def discard(target: String): this.type = {
        discard(IdentifierTag(target))
    }

    private def accept(target: EngineTag): this.type = {
        new GenericRMIRulesAgreementBuilder(discarded.filterNot(target.equals), accepted :+ target, conditions, acceptAllTargets, desiredEngineReturn)
    }

    override def accept(target: String): this.type = {
        accept(IdentifierTag(target))
    }

    override def acceptOwner(): this.type = {
        accept(OwnerEngine)
    }

    override def acceptAll(): this.type = {
        new GenericRMIRulesAgreementBuilder(Seq.empty, Seq.empty, conditions, true, desiredEngineReturn)
    }

    override def discardAll(): this.type = {
        new GenericRMIRulesAgreementBuilder(Seq.empty, Seq.empty, conditions, false, desiredEngineReturn)
    }

    override def acceptCurrent(): this.type = accept(CurrentEngine)

    override def discardCurrent(): this.type = discard(CurrentEngine)

    override def discardOwner(): this.type = discard(OwnerEngine)

    override def acceptCacheOwner(): this.type = accept(CacheOwnerEngine)

    override def discardCacheOwner(): this.type = discard(CacheOwnerEngine)

    override def setDesiredEngineReturn(target: String): this.type = {
        setDesiredEngineReturn(IdentifierTag(target))
    }

    private def setDesiredEngineReturn(target: EngineTag): this.type = {
        new GenericRMIRulesAgreementBuilder(Seq.empty, Seq.empty, conditions, acceptAllTargets, target)
    }

    override def desireOwnerEngineToReturn(): this.type = {
        setDesiredEngineReturn(OwnerEngine)
    }

    private def addCondition(condition: AgreementCondition, action: AgreementConditionAction): GenericRMIRulesAgreementBuilder = {
        val conditions = this.conditions :+ ((context: AgreementContext) => condition(context, this, action))
        new GenericRMIRulesAgreementBuilder(Seq.empty, Seq.empty, conditions, acceptAllTargets, desiredEngineReturn)
    }

    override def ifCurrentIsRootOwner(action: AgreementConditionAction): this.type = {
        addCondition(CurrentIsRootOwner, action)
    }

    override def ifCurrentIsNotRootOwner(action: AgreementConditionAction): this.type = {
        addCondition(CurrentIsNotRootOwner, action)
    }

    override def ifCurrentIsOwner(action: AgreementConditionAction): this.type = {
        addCondition(CurrentIsOwner, action)
    }

    override def ifCurrentIsNotOwner(action: AgreementConditionAction): this.type = {
        addCondition(CurrentIsNotOwner, action)
    }

    override def ifCurrentIsCacheOwner(action: AgreementConditionAction): this.type = {
        addCondition(CurrentIsCacheOwner, action)
    }

    override def ifCurrentIsNotCacheOwner(action: AgreementConditionAction): this.type = {
        addCondition(CurrentIsNotCacheOwner, action)
    }

    def result(context: AgreementContext): RMIRulesAgreement = {
        var builder: GenericRMIRulesAgreementBuilder = this
        for (condition <- conditions) {
            builder = condition(context)
        }
        val desiredEngineReturnIdentifier = context.translate(builder.desiredEngineReturn)
        val accepted = builder.accepted.map(context.translate).toArray
        val discarded = builder.discarded.map(context.translate).toArray
        new UsageRMIRulesAgreement(context.currentID, context.ownerID, desiredEngineReturnIdentifier, acceptAllTargets, accepted, discarded)
    }

}

object GenericRMIRulesAgreementBuilder {

    type AgreementCondition = (AgreementContext, GenericRMIRulesAgreementBuilder, GenericRMIRulesAgreementBuilder => GenericRMIRulesAgreementBuilder) => GenericRMIRulesAgreementBuilder
    type AgreementConditionResult = AgreementContext => GenericRMIRulesAgreementBuilder
    private final val CurrentIsCacheOwner   : AgreementCondition = (c, b, e) => if (c.currentIsCacheOwner) e(b) else b
    private final val CurrentIsNotCacheOwner: AgreementCondition = (c, b, e) => if (!c.currentIsCacheOwner) e(b) else b
    private final val CurrentIsOwner        : AgreementCondition = (c, b, e) => if (c.currentIsOwner) e(b) else b
    private final val CurrentIsNotOwner     : AgreementCondition = (c, b, e) => if (!c.currentIsOwner) e(b) else b
    private final val CurrentIsRootOwner    : AgreementCondition = (c, b, e) => if (c.currentIsRootOwner) e(b) else b
    private final val CurrentIsNotRootOwner : AgreementCondition = (c, b, e) => if (!c.currentIsRootOwner) e(b) else b

    implicit private def fastWrap(in: () => Unit): Boolean = true


}
