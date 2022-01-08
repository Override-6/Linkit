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

abstract class LambdaFieldModifier[F <: AnyRef] extends ValueModifier[F] {

    private var fromRemote0     : (F, Engine) => F    = (f, _) => f
    private var fromRemoteEvent0: (F, Engine) => Unit = (_, _) => ()

    protected def fromRemote_=(f: (F, Engine) => F): Unit = fromRemote0 = f

    protected def fromRemoteEvent_=(f: (F, Engine) => Unit): Unit = fromRemoteEvent0 = f

    override final def fromRemote(receivedField: F, remote: Engine): F = {
        fromRemote0(receivedField, remote)
    }

    override final def fromRemoteEvent(receivedField: F, remote: Engine): Unit = {
        fromRemoteEvent0(receivedField, remote)
    }
}
