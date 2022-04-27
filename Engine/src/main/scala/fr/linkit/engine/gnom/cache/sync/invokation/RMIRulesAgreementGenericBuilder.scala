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
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder._

class RMIRulesAgreementGenericBuilder private(private val discarded: Seq[EngineTag],
                                              private val accepted: Seq[EngineTag],
                                              private val conditions: Seq[AgreementConditionResult],
                                              private val acceptAllTargets: Boolean,
                                              private val desiredEngineReturn: EngineTag) extends RMIRulesAgreementBuilder {

    def this() {
        this(Seq.empty, Seq(CurrentEngine), Seq.empty, false, CurrentEngine)
    }

    def this(other: RMIRulesAgreementGenericBuilder) {
        this(other.discarded, other.accepted, other.conditions, other.acceptAllTargets, other.desiredEngineReturn)
    }

    override def discard(target: EngineTag): RMIRulesAgreementGenericBuilder = {
        new RMIRulesAgreementGenericBuilder(discarded :+ target, accepted.filterNot(target.equals), conditions, acceptAllTargets, desiredEngineReturn)
    }

    override def accept(target: EngineTag): RMIRulesAgreementGenericBuilder = {
        new RMIRulesAgreementGenericBuilder(discarded.filterNot(target.equals), accepted :+ target, conditions, acceptAllTargets, desiredEngineReturn)
    }

    override def acceptAll(): RMIRulesAgreementGenericBuilder = {
        new RMIRulesAgreementGenericBuilder(Seq(), accepted, conditions, true, desiredEngineReturn)
    }

    override def discardAll(): RMIRulesAgreementGenericBuilder = {
        new RMIRulesAgreementGenericBuilder(discarded, Seq(), conditions, false, desiredEngineReturn)
    }

    override def appointReturn(target: EngineTag): RMIRulesAgreementGenericBuilder = {
        new RMIRulesAgreementGenericBuilder(discarded, accepted, conditions, acceptAllTargets, target)
    }

    private def addCondition(condition: AgreementCondition, ifTrue: AgreementConditionAction, ifFalse: AgreementConditionAction): RMIRulesAgreementGenericBuilder = {
        val conditions = this.conditions :+ ((context: SyncObjectContext) => condition(context, this, ifTrue, ifFalse))
        new RMIRulesAgreementGenericBuilder(discarded, accepted, conditions, acceptAllTargets, desiredEngineReturn)
    }

    override def assuming(left: EngineTag): Condition = new GenericCondition(left)

    class GenericCondition(left: EngineTag) extends Condition {

        override def is(right: EngineTag): AgreementConditionAction => RMIRulesAgreementBuilder = {
            addCondition(compare(left, right), _, x => x)
        }

        override def isNot(right: EngineTag): (AgreementConditionAction) => RMIRulesAgreementBuilder = {
            addCondition(compareNot(left, right), _, x => x)
        }

        override def isElse(right: EngineTag): (AgreementConditionAction, AgreementConditionAction) => RMIRulesAgreementBuilder = {
            addCondition(compare(left, right), _, _)
        }

        override def isNotElse(right: EngineTag): (AgreementConditionAction, AgreementConditionAction) => RMIRulesAgreementBuilder = {
            addCondition(compareNot(left, right), _, _)
        }
    }

    override def result(context: SyncObjectContext): RMIRulesAgreement = {
        var builder = this
        for (condition <- conditions) {
            builder = condition(context).asInstanceOf[RMIRulesAgreementGenericBuilder]
        }
        val desiredEngineReturnIdentifier = context.translate(builder.desiredEngineReturn)
        val accepted                      = builder.accepted.map(context.translate).toArray
        val discarded                     = builder.discarded.map(context.translate).toArray
        new UsageRMIRulesAgreement(
            context.currentID, context.ownerID, desiredEngineReturnIdentifier,
            builder.acceptAllTargets, accepted, discarded)
    }

}

object RMIRulesAgreementGenericBuilder {

    final val EmptyBuilder = new RMIRulesAgreementGenericBuilder()

    type Action = RMIRulesAgreementBuilder => RMIRulesAgreementBuilder
    type AgreementCondition = (SyncObjectContext, RMIRulesAgreementBuilder, Action, Action) => RMIRulesAgreementBuilder
    type AgreementConditionResult = SyncObjectContext => RMIRulesAgreementBuilder

    private final def compare(left: EngineTag, right: EngineTag): AgreementCondition = (c, b, ifTrue, ifFalse) => if (c.areEquals(right, left)) ifTrue(b) else ifFalse(b)

    private final def compareNot(left: EngineTag, right: EngineTag): AgreementCondition = (c, b, ifTrue, ifFalse) => if (!c.areEquals(right, left)) ifTrue(b) else ifFalse(b)

    implicit private def fastWrap(in: () => Unit): Boolean = true

}
