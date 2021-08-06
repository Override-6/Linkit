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

import fr.linkit.api.connection.cache.obj.invokation.{WrapperMethodInvocation, MethodInvocationHandler}
import fr.linkit.api.local.system.AppLogger

object InvokeOnlyRMIHandler extends MethodInvocationHandler {

    override def handleRMI[R](invocation: WrapperMethodInvocation[R]): R = {

        val args           = invocation.methodArguments
        val methodBehavior = invocation.methodBehavior
        val methodID       = invocation.methodID
        val puppeteer      = invocation.wrapper.getPuppeteer

        val enableDebug = invocation.debug

        if (enableDebug)
            AppLogger.debug("Invoke Only: Sending invocation request.")
        puppeteer.sendInvoke(invocation)
        var localResult: Any = methodBehavior.defaultReturnValue
        if (methodBehavior.invocationKind.isLocalInvocationForced) {
            if (enableDebug)
                AppLogger.debug("The call is also redirected to current object...")
            localResult = invocation.callSuper()
        }
        if (enableDebug)
            AppLogger.debug("Returned local result = " + localResult)
        //# Note1: value of 'InvokeOnlyResult' can be "localResult", which will return the variable.
        //# Note2: Be aware that you can get a null value returned
        //#        if the 'InvokeOnlyResult' value of the annotated
        //#        method is set to "localResult" and the invocation
        //#        kind does not force local invocation.
        localResult.asInstanceOf[R]
    }
}
