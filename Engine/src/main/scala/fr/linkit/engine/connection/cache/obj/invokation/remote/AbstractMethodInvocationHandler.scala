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

package fr.linkit.engine.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreement
import fr.linkit.api.connection.cache.obj.behavior.member.MethodBehavior
import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.remote.{Puppeteer, RemoteMethodInvocation, RemoteMethodInvocationHandler}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.invokation.{AbstractMethodInvocation, SimpleRMIRulesAgreementBuilder}

abstract class AbstractMethodInvocationHandler extends RemoteMethodInvocationHandler {

    override def handleRMI[R](localInvocation: LocalMethodInvocation[R]): R = {
        val wrapper           = localInvocation.synchronizedObject
        val methodBehavior    = localInvocation.methodBehavior
        val puppeteer         = wrapper.getPuppeteer
        val currentIdentifier = puppeteer.currentIdentifier
        val args              = localInvocation.methodArguments
        val name              = methodBehavior.desc.javaMethod.getName
        val methodID          = localInvocation.methodID

        val enableDebug = localInvocation.debug

        lazy val argsString = args.mkString("(", ", ", ")")
        lazy val className  = methodBehavior.desc.classDesc.clazz
        if (enableDebug) {
            AppLogger.debug(s"$name: Performing rmi call for ${className.getSimpleName}.$name$argsString (id: $methodID)")
            AppLogger.debug(s"MethodBehavior = $methodBehavior")
        }
        // From here we are sure that we want to perform a remote
        // method invocation. (An invocation to the current machine (invocation.callSuper()) can be added).
        var result     : Any = methodBehavior.defaultReturnValue
        var localResult: Any = result
        val methodAgreement  = methodBehavior.completeAgreement(new SimpleRMIRulesAgreementBuilder(wrapper.getPuppeteer.ownerID, localInvocation.currentIdentifier))
        val mayPerformRMI    = methodAgreement.mayPerformRemoteInvocation
        if (methodAgreement.mayCallSuper) {
            localResult = localInvocation.callSuper()
        }
        val remoteInvocation = new AbstractMethodInvocation[R](localInvocation) with RemoteMethodInvocation[R] {
            override val agreement: RMIRulesAgreement = methodAgreement

            override def dispatchRMI(dispatcher: Puppeteer[_]#RMIDispatcher): Unit = {
                methodBehavior.dispatch(dispatcher)
            }
        }
        if (methodAgreement.getDesiredEngineReturn == currentIdentifier) {
            if (mayPerformRMI)
                voidRMIInvocation(puppeteer, remoteInvocation)
            result = localResult
        } else if (mayPerformRMI) {
            result = puppeteer.sendInvokeAndWaitResult(remoteInvocation)
        }
        result.asInstanceOf[R]
    }


    protected def voidRMIInvocation(puppeteer: Puppeteer[_], invocation: RemoteMethodInvocation[_]): Unit
}
