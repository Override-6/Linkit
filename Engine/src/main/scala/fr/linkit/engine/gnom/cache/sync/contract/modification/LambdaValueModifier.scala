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
import fr.linkit.api.gnom.network.Engine

abstract class LambdaValueModifier[A <: AnyRef] extends ValueModifier[A] {

    protected var remoteToCurrent     : (A, Engine) => A    = (receivedParam, _) => receivedParam
    protected var currentToRemote     : (A, Engine) => A    = (localParam, _) => localParam
    protected var remoteToCurrentEvent: (A, Engine) => Unit = (_, _) => ()
    protected var currentToRemoteEvent: (A, Engine) => Unit = (_, _) => ()

    override final def fromRemote(receivedParam: A, remote: Engine): A =
        remoteToCurrent(receivedParam, remote)

    override final def toRemote(localParam: A, remote: Engine): A =
        currentToRemote(localParam, remote)

    override final def fromRemoteEvent(receivedParam: A, remote: Engine): Unit = {
        remoteToCurrentEvent(receivedParam, remote)
    }

    override final def toRemoteEvent(localParam: A, remote: Engine): Unit = {
        currentToRemoteEvent(localParam, remote)
    }
}
