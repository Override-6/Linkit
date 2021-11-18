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

package fr.linkit.engine.gnom.cache.sync.contract.behavior.builder

import fr.linkit.api.gnom.cache.sync.contract.ParameterContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.RemoteInvocationRule
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.ReturnValueBehavior
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contract.modification.MethodCompModifier
import fr.linkit.api.gnom.cache.sync.invokation.remote.MethodInvocationHandler
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.invokation.DefaultMethodInvocationHandler
import org.jetbrains.annotations.Nullable

case class MethodContractDescriptor(description: MethodDescription, rule: RemoteInvocationRule,
                                    isActivated: Boolean,
                                    parameterContracts: Array[ParameterContract[Any]],
                                    @Nullable returnValueBehavior: ReturnValueBehavior[Any],
                                    @Nullable returnValueModifier: MethodCompModifier[Any],
                                    isHidden: Boolean,
                                    forceLocalInnerInvocations: Boolean,
                                    @Nullable procrastinator: Procrastinator,
                                    @Nullable handler: MethodInvocationHandler) {

    def this(description: MethodDescription, rule: RemoteInvocationRule, isActivated: Boolean) {
        this(description, rule, isActivated, Array.empty, null, null, false, false, null, DefaultMethodInvocationHandler)
    }

    def this(description: MethodDescription, other: MethodContractDescriptor) {
        this(description, other.rule, other.isActivated, other.parameterContracts, other.returnValueBehavior,
            other.returnValueModifier, other.isHidden, other.forceLocalInnerInvocations, other.procrastinator, other.handler)
    }
}