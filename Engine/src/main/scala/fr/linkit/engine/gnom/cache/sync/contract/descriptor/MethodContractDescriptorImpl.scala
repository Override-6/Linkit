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

import fr.linkit.api.gnom.cache.sync.contract.ParameterContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.MethodBehavior
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contract.descriptors.MethodContractDescriptor
import fr.linkit.api.gnom.cache.sync.contractv2.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.invocation.remote.MethodInvocationHandler
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

case class MethodContractDescriptorImpl(override val description: MethodDescription,
                                        @Nullable override val procrastinator: Procrastinator,
                                        @Nullable override val handler: MethodInvocationHandler,
                                        @Nullable returnValueModifier: ValueModifier[Any],
                                        override val parameterContracts : Array[ParameterContract[Any]],
                                        override val behavior: MethodBehavior) extends MethodContractDescriptor {

    def this(description: MethodDescription, other: MethodContractDescriptor) {
        this(description, other.procrastinator, other.handler, other.returnValueModifier, other.parameterContracts, other.behavior)
    }

}