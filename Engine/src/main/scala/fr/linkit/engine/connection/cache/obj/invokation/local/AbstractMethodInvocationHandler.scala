/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.obj.invokation.local

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreement
import fr.linkit.api.connection.cache.obj.invokation.remote.{Puppeteer, RemoteMethodInvocationHandler, SynchronizedMethodInvocation}
import fr.linkit.api.local.system.AppLogger

abstract class AbstractMethodInvocationHandler extends RemoteMethodInvocationHandler {

    override def handleRMI[R](agreement: RMIRulesAgreement, invocation: SynchronizedMethodInvocation[R]): R = {
        val wrapper        = invocation.synchronizedObject
        val args           = invocation.methodArguments
        val methodBehavior = invocation.methodBehavior
        val name           = methodBehavior.desc.javaMethod.getName
        val methodID       = invocation.methodID

        val enableDebug = invocation.debug

        lazy val argsString = args.mkString("(", ", ", ")")
        lazy val className  = methodBehavior.desc.classDesc.clazz
        if (enableDebug) {
            AppLogger.debug(s"$name: Performing rmi call for ${className.getSimpleName}.$name$argsString (id: $methodID)")
            AppLogger.debug(s"MethodBehavior = $methodBehavior")
        }
        // From here we are sure that we want to perform a remote
        // method invocation. (An invocation to the current machine (invocation.callSuper()) can be added).
        val puppeteer        = wrapper.getPuppeteer
        var result     : Any = methodBehavior.defaultReturnValue
        var localResult: Any = result
        val mayPerformRMI    = agreement.mayPerformRemoteInvocation
        if (agreement.mayCallSuper) {
            localResult = invocation.callSuper()
        }
        if (agreement.getDesiredEngineReturn == invocation.currentIdentifier) {
            if (mayPerformRMI)
                voidRMIInvocation(puppeteer, agreement, invocation)
            result = localResult
        } else if (mayPerformRMI) {
            result = puppeteer.sendInvokeAndWaitResult(agreement, invocation)
        }
        result.asInstanceOf[R]
    }

    def voidRMIInvocation(puppeteer: Puppeteer[_], agreement: RMIRulesAgreement, invocation: SynchronizedMethodInvocation[_]): Unit
}
