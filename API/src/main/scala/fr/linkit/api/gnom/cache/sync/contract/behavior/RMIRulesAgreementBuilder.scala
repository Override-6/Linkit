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

package fr.linkit.api.gnom.cache.sync.contract.behavior

trait RMIRulesAgreementBuilder {

    type AgreementConditionAction = RMIRulesAgreementBuilder => RMIRulesAgreementBuilder

    def discard(target: EngineTag): RMIRulesAgreementBuilder

    def accept(target: EngineTag): RMIRulesAgreementBuilder

    def acceptAll(): RMIRulesAgreementBuilder

    def discardAll(): RMIRulesAgreementBuilder

    def appointReturn(target: EngineTag): RMIRulesAgreementBuilder

    def assuming(left: EngineTag): Condition

    def result(context: ConnectedObjectContext): RMIRulesAgreement

    trait Condition {

        def is(right: EngineTag, ifTrue: AgreementConditionAction): RMIRulesAgreementBuilder = is(right)(ifTrue)

        def isNot(right: EngineTag, ifTrue: AgreementConditionAction): RMIRulesAgreementBuilder = isNot(right)(ifTrue)

        def isElse(right: EngineTag, ifTrue: AgreementConditionAction, ifFalse: AgreementConditionAction): RMIRulesAgreementBuilder = isElse(right)(ifTrue, ifFalse)

        def isNotElse(right: EngineTag, ifTrue: AgreementConditionAction, ifFalse: AgreementConditionAction): RMIRulesAgreementBuilder = isNotElse(right)(ifTrue, ifFalse)

        def is(right: EngineTag): AgreementConditionAction => RMIRulesAgreementBuilder

        def isNot(right: EngineTag): AgreementConditionAction => RMIRulesAgreementBuilder

        def isElse(right: EngineTag): (AgreementConditionAction, AgreementConditionAction) => RMIRulesAgreementBuilder

        def isNotElse(right: EngineTag): (AgreementConditionAction, AgreementConditionAction) => RMIRulesAgreementBuilder
    }

}

