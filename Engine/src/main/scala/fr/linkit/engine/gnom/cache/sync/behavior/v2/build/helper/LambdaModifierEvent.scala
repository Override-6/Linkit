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

package fr.linkit.engine.gnom.cache.sync.behavior.v2.build.helper

import fr.linkit.api.gnom.cache.sync.behavior.member.method.MethodCompModifier
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.network.Engine

class LambdaModifierEvent[A] extends MethodCompModifier[A] {

    protected var currentToCurrent: (A, LocalMethodInvocation[_]) => Unit         = (localParam, _) => localParam
    protected var remoteToCurrent : (A, LocalMethodInvocation[_], Engine) => Unit = (receivedParam, _, _) => receivedParam
    protected var toRemote        : (A, LocalMethodInvocation[_], Engine) => Unit = (localParam, _, _) => localParam

    override def forLocalComingFromLocal(localParam: A, invocation: LocalMethodInvocation[_]): A = {
        currentToCurrent(localParam, invocation)
        localParam
    }

    override def forLocalComingFromRemote(receivedParam: A, invocation: LocalMethodInvocation[_], remote: Engine): A = {
        remoteToCurrent(receivedParam, invocation, remote)
        receivedParam
    }

    override def forRemote(localParam: A, invocation: LocalMethodInvocation[_], remote: Engine): A = {
        toRemote(localParam, invocation, remote)
        localParam
    }
}
