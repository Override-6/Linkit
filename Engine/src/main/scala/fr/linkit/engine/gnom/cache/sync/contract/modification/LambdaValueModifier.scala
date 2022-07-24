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

    override final def toRemote(input: A, remote: Engine): A = {
        currentToRemote(input, remote)
    }

    protected def toRemote_=(f: (A, Engine) => A): Unit = currentToRemote = f

    def toRemote: (A, Engine) => A = currentToRemote

    override final def fromRemote(input: A, remote: Engine): A = {
        remoteToCurrent(input, remote)
    }

    protected def fromRemote_=(f: (A, Engine) => A): Unit = remoteToCurrent = f

    protected def fromRemote: (A, Engine) => A = remoteToCurrent


}
