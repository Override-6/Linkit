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

package fr.linkit.engine.gnom.cache.sync.contract.modification

import fr.linkit.api.gnom.cache.sync.contractv2.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.invocation.local.LocalMethodInvocation
import fr.linkit.api.gnom.network.Engine

abstract class LambdaValueModifier[A <: AnyRef] extends ValueModifier[A] {

    protected var remoteToCurrent     : (A, LocalMethodInvocation[_], Engine) => A    = (receivedParam, _, _) => receivedParam
    protected var currentToRemote     : (A, LocalMethodInvocation[_], Engine) => A    = (localParam, _, _) => localParam
    protected var remoteToCurrentEvent: (A, LocalMethodInvocation[_], Engine) => Unit = (_, _, _) => ()
    protected var currentToRemoteEvent: (A, LocalMethodInvocation[_], Engine) => Unit = (_, _, _) => ()

    override final def fromRemote(receivedParam: A, invocation: LocalMethodInvocation[_], remote: Engine): A =
        remoteToCurrent(receivedParam, invocation, remote)

    override final def toRemote(localParam: A, invocation: LocalMethodInvocation[_], remote: Engine): A =
        currentToRemote(localParam, invocation, remote)

    override final def fromRemoteEvent(receivedParam: A, invocation: LocalMethodInvocation[_], remote: Engine): Unit = {
        remoteToCurrentEvent(receivedParam, invocation, remote)
    }

    override final def toRemoteEvent(localParam: A, invocation: LocalMethodInvocation[_], remote: Engine): Unit = {
        currentToRemoteEvent(localParam, invocation, remote)
    }
}
