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
import fr.linkit.api.gnom.network.tag.{Current, EngineTag, Nobody, UniqueTag}

class RMIRulesAgreementGenericBuilder private(private val selectionTag       : EngineTag,
                                              private val desiredEngineReturn: UniqueTag) extends RMIRulesAgreementBuilder {

    def this() {
        this(Nobody, Current)
    }

    def this(other: RMIRulesAgreementGenericBuilder) {
        this(other.selectionTag, other.desiredEngineReturn)
    }


    override def selection(tagSelection: EngineTag): RMIRulesAgreementBuilder = {
        new RMIRulesAgreementGenericBuilder(tagSelection, desiredEngineReturn)
    }

    override def appointReturn(target: UniqueTag): RMIRulesAgreementGenericBuilder = {
        new RMIRulesAgreementGenericBuilder(selectionTag, target)
    }

    override def result(context: ConnectedObjectContext): RMIDispatchAgreement = {
        new UsageRMIDispatchAgreement(context, desiredEngineReturn, context.deepTranslate(selectionTag))
    }

}