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

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.field.FieldModifier
import fr.linkit.api.gnom.network.Engine

abstract class LambdaFieldModifier[F <: AnyRef] extends FieldModifier[F] {
    protected var fromRemote     : (F, SynchronizedObject[_], Engine) => F    = (f, _, _) => f
    protected var fromRemoteEvent: (F, SynchronizedObject[_], Engine) => Unit = (_, _, _) => ()

    override final def receivedFromRemote(receivedField: F, containingObject: SynchronizedObject[_], remote: Engine): F = {
        fromRemote(receivedField, containingObject, remote)
    }

    override final def receivedFromRemoteEvent(receivedField: F, containingObject: SynchronizedObject[_], remote: Engine): Unit = {
        fromRemoteEvent(receivedField, containingObject, remote)
    }
}
