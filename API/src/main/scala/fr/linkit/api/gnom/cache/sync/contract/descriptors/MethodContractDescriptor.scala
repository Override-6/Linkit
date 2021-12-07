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

package fr.linkit.api.gnom.cache.sync.contract.descriptors

import fr.linkit.api.gnom.cache.sync.contract.ParameterContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.MethodBehavior
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contract.modification.MethodCompModifier
import fr.linkit.api.gnom.cache.sync.invokation.remote.MethodInvocationHandler
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

trait MethodContractDescriptor {

              val description        : MethodDescription
              val parameterContracts : Array[ParameterContract[Any]]
    @Nullable val procrastinator     : Procrastinator
    @Nullable val handler            : MethodInvocationHandler
    @Nullable val returnValueModifier: MethodCompModifier[Any]
              val behavior           : MethodBehavior
}
