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

package fr.linkit.api.gnom.cache.sync.contractv2

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingData
import fr.linkit.api.gnom.network.Engine

trait MethodContract[R] {

    val isRMIActivated: Boolean

    def handleInvocationResult(initialResult: Any, remote: Engine)(syncAction: AnyRef => SynchronizedObject[AnyRef]): Any

    def synchronizeArguments(args: Array[Any], syncAction: AnyRef => SynchronizedObject[AnyRef]): Array[Any]

    def handleRMI(data: InvocationHandlingData): R

}
