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

import fr.linkit.api.gnom.cache.sync.contract.ValueContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreementBuilder
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contract.descriptors.MethodContractDescriptor
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

case class MethodContractDescriptorImpl(description: MethodDescription,
                                        procrastinator: Option[Procrastinator],
                                        returnValueContract: Option[ValueContract[Any]],
                                        parameterContracts: Array[ValueContract[Any]],
                                        hideMessage: Option[String],
                                        forceLocalInnerInvocations: Boolean,
                                        agreement: RMIRulesAgreementBuilder) extends MethodContractDescriptor {

    def this(description: MethodDescription, other: MethodContractDescriptor) {
        this(description, other.procrastinator, other.returnValueContract, other.parameterContracts, other.hideMessage, other.forceLocalInnerInvocations, other.agreement)
    }

}