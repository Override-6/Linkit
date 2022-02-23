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

import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.network.Engine

abstract class LambdaValueModifier[A <: AnyRef] extends ValueModifier[A] {

    private var remoteToCurrent     : (A, Engine) => A    = (receivedParam, _) => receivedParam
    private var currentToRemote     : (A, Engine) => A    = (localParam, _) => localParam
    private var remoteToCurrentEvent: (A, Engine) => Unit = (_, _) => ()
    private var currentToRemoteEvent: (A, Engine) => Unit = (_, _) => ()

    override final def toRemote(localParam: A, remote: Engine): A = {
        currentToRemote(localParam, remote)
    }

    protected def toRemote_=(f: (A, Engine) => A): Unit = currentToRemote = f

    def toRemote: (A, Engine) => A = currentToRemote

    override final def fromRemote(receivedParam: A, remote: Engine): A = {
        remoteToCurrent(receivedParam, remote)
    }

    protected def fromRemote_=(f: (A, Engine) => A): Unit = remoteToCurrent = f

    protected def fromRemote: (A, Engine) => A = remoteToCurrent

    override final def fromRemoteEvent(receivedParam: A, remote: Engine): Unit = {
        remoteToCurrentEvent(receivedParam, remote)
    }

    protected def fromRemoteEvent_=(f: (A, Engine) => Unit): Unit = remoteToCurrentEvent = f

    protected def fromRemoteEvent: (A, Engine) => Unit = remoteToCurrentEvent

    override final def toRemoteEvent(localParam: A, remote: Engine): Unit = {
        currentToRemoteEvent(localParam, remote)
    }

    protected def toRemoteEvent_=(f: (A, Engine) => Unit): Unit = currentToRemoteEvent = f

    protected def toRemoteEvent: (A, Engine) => Unit = currentToRemoteEvent

}
