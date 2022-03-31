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

import fr.linkit.api.gnom.cache.sync.contract.ModifiableValueContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreementBuilder
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contract.descriptor.MethodContractDescriptor
import fr.linkit.api.internal.concurrency.Procrastinator

case class MethodContractDescriptorImpl(description: MethodDescription,
                                        forced: Boolean,
                                        procrastinator: Option[Procrastinator],
                                        returnValueContract: Option[ModifiableValueContract[Any]],
                                        parameterContracts: Array[ModifiableValueContract[Any]],
                                        hideMessage: Option[String],
                                        skipInnerInvocations: Boolean,
                                        agreement: RMIRulesAgreementBuilder) extends MethodContractDescriptor {

    def this(description: MethodDescription, other: MethodContractDescriptor) {
        this(description, other.forced, other.procrastinator, other.returnValueContract, other.parameterContracts, other.hideMessage, other.skipInnerInvocations, other.agreement)
    }

}