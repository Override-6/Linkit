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

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior._
import fr.linkit.api.gnom.network.{Current, EngineTag, UniqueTag}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder._

class RMIRulesAgreementGenericBuilder private(private val discarded: Seq[EngineTag],
                                              private val accepted: Seq[EngineTag],
                                              private val conditions: Seq[AgreementConditionResult],
                                              private val acceptAllTargets: Boolean,
                                              private val desiredEngineReturn: UniqueTag) extends RMIRulesAgreementBuilder {

    def this() {
        this(Seq.empty, Seq(), Seq.empty, false, Current)
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

    override def appointReturn(target: UniqueTag): RMIRulesAgreementGenericBuilder = {
        new RMIRulesAgreementGenericBuilder(discarded, accepted, conditions, acceptAllTargets, target)
    }

    private def addCondition(condition: AgreementCondition, ifTrue: AgreementConditionAction, ifFalse: AgreementConditionAction): RMIRulesAgreementGenericBuilder = {
        val conditions = this.conditions :+ ((context: ConnectedObjectContext) => condition(context, this, ifTrue, ifFalse))
        new RMIRulesAgreementGenericBuilder(discarded, accepted, conditions, acceptAllTargets, desiredEngineReturn)
    }

    override def assuming(left: EngineTag): Condition = new GenericCondition(left)

    class GenericCondition(left: EngineTag) extends Condition {

        override def is(right: EngineTag): AgreementConditionAction => RMIRulesAgreementBuilder = {
            addCondition(compare(left, right), _, x => x)
        }

        override def isNot(right: EngineTag): AgreementConditionAction => RMIRulesAgreementBuilder = {
            addCondition(compareNot(left, right), _, x => x)
        }

        override def isElse(right: EngineTag): (AgreementConditionAction, AgreementConditionAction) => RMIRulesAgreementBuilder = {
            addCondition(compare(left, right), _, _)
        }

        override def isNotElse(right: EngineTag): (AgreementConditionAction, AgreementConditionAction) => RMIRulesAgreementBuilder = {
            addCondition(compareNot(left, right), _, _)
        }
    }

    override def result(context: ConnectedObjectContext): RMIDispatchAgreement = {
        var builder = this
        for (condition <- conditions) {
            builder = condition(context).asInstanceOf[RMIRulesAgreementGenericBuilder]
        }
        val desiredEngineReturn = context.translate(this.desiredEngineReturn)
        val accepted            = context.translateAll(builder.accepted)
        val discarded           = context.translateAll(builder.discarded)
        new UsageRMIDispatchAgreement(context, desiredEngineReturn,
            builder.acceptAllTargets, accepted, discarded)
    }

}

object RMIRulesAgreementGenericBuilder {

    final val EmptyBuilder = new RMIRulesAgreementGenericBuilder()

    type Action = RMIRulesAgreementBuilder => RMIRulesAgreementBuilder
    type AgreementCondition = (ConnectedObjectContext, RMIRulesAgreementBuilder, Action, Action) => RMIRulesAgreementBuilder
    type AgreementConditionResult = ConnectedObjectContext => RMIRulesAgreementBuilder

    private final def compare(left: EngineTag, right: EngineTag): AgreementCondition = (c, b, ifTrue, ifFalse) => if (c.areEquivalent(right, left)) ifTrue(b) else ifFalse(b)

    private final def compareNot(left: EngineTag, right: EngineTag): AgreementCondition = (c, b, ifTrue, ifFalse) => if (!c.areEquivalent(right, left)) ifTrue(b) else ifFalse(b)

    implicit private def fastWrap(in: () => Unit): Boolean = true

}
