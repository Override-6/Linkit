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

package fr.linkit.api.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.invocation.{InvocationChoreographer, InvocationHandlingMethod}
import fr.linkit.api.gnom.cache.sync.{ChippedObject, ConnectedObject}
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.ObjectConnector
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

trait MethodContract[R] {

    @Nullable val procrastinator: Procrastinator

    val hideMessage: Option[String]

    val isRMIActivated: Boolean

    val invocationHandlingMethod: InvocationHandlingMethod

    val choreographer: InvocationChoreographer

    def handleInvocationResult(initialResult: Any, remote: Engine)(syncAction: (AnyRef, RegistrationKind) => ConnectedObject[AnyRef]): Any

    def connectArgs(args: Array[Any], syncAction: (Any, RegistrationKind) => ConnectedObject[AnyRef]): Unit

    def applyReturnValue(rv: Any, syncAction: (Any, RegistrationKind) => ConnectedObject[AnyRef]): Any

    def executeRemoteMethodInvocation(data: RemoteInvocationExecution): R

    def executeMethodInvocation(sender: Engine, data: InvocationExecution): Any

    trait InvocationExecution {

        val obj      : ChippedObject[_]
        val arguments: Array[Any]
    }

    trait RemoteInvocationExecution extends InvocationExecution {

        val puppeteer: Puppeteer[_]

        val connector: ObjectConnector

        def doSuperCall(): Any
    }

}
