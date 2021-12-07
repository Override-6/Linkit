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

package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.behavior._
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method._
import fr.linkit.engine.gnom.cache.sync.contract.behavior.member.DefaultUsageMethodBehavior

class DefaultGenericMethodBehavior(val isActivated: Boolean,
                                   val isHidden: Boolean,
                                   val forceLocalInnerInvocations: Boolean,
                                   val parameterBehaviors: Array[ParameterBehavior[Any]],
                                   val returnValueBehavior: ReturnValueBehavior[Any],
                                   val agreementBuilder: RMIRulesAgreementBuilder) extends GenericMethodBehavior {

    override def toUsage(context: SyncObjectContext): UsageMethodBehavior = {
        val agreement = agreementBuilder.result(context)
        DefaultUsageMethodBehavior(isActivated, parameterBehaviors, returnValueBehavior, isHidden, forceLocalInnerInvocations, agreement)
    }

}
