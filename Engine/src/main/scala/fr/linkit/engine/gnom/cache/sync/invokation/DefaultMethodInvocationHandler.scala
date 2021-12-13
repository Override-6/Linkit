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

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreement
import fr.linkit.api.gnom.cache.sync.contract.modification.MethodCompModifierKind
import fr.linkit.api.gnom.cache.sync.invokation.local.{CallableLocalMethodInvocation, LocalMethodInvocation}
import fr.linkit.api.gnom.cache.sync.invokation.remote.{DispatchableRemoteMethodInvocation, MethodInvocationHandler, Puppeteer}
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.internal.system.AppLogger

object DefaultMethodInvocationHandler extends MethodInvocationHandler {

    override def handleRMI[R](localInvocation: CallableLocalMethodInvocation[R]): R = {
        val syncNode   = localInvocation.objectNode
        val syncObject = syncNode.synchronizedObject
        val contract   = localInvocation.methodContract
        val behavior   = contract.behavior
        val desc       = contract.description
        val puppeteer  = syncObject.getPuppeteer
        val args       = localInvocation.methodArguments
        val name       = desc.javaMethod.getName
        val methodID   = localInvocation.methodID
        val network    = puppeteer.network

        val currentIdentifier = puppeteer.currentIdentifier
        //val rootOwnerIdentifier = syncNode.tree.rootNode.ownerID

        val enableDebug = localInvocation.debug
        if (enableDebug) {
            val argsString = args.mkString("(", ", ", ")")
            val className  = desc.classDesc.clazz
            AppLogger.debug(s"$name: Performing rmi call for ${className.getSimpleName}.$name$argsString (id: $methodID)")
            AppLogger.debug(s"MethodBehavior = $behavior")
        }
        // From here we are sure that we want to perform a remote
        // method invocation. (An invocation to the current machine (invocation.callSuper()) can be added).
        var result     : Any = null
        var localResult: Any = result
        val methodAgreement  = behavior.agreement
        val mayPerformRMI    = methodAgreement.mayPerformRemoteInvocation
        if (methodAgreement.mayCallSuper) {
            localResult = localInvocation.callSuper()
        }
        val remoteInvocation = new AbstractMethodInvocation[R](localInvocation) with DispatchableRemoteMethodInvocation[R] {
            override val agreement: RMIRulesAgreement = methodAgreement

            override def dispatchRMI(dispatcher: Puppeteer[AnyRef]#RMIDispatcher): Unit = {
                makeDispatch(dispatcher, network, localInvocation)
            }
        }
        if (methodAgreement.getDesiredEngineReturn == currentIdentifier) {
            if (mayPerformRMI)
                puppeteer.sendInvoke(remoteInvocation)
            result = localResult
        } else if (mayPerformRMI) {
            result = puppeteer.sendInvokeAndWaitResult(remoteInvocation)
        }
        result.asInstanceOf[R]
    }

    private def makeDispatch(dispatcher: Puppeteer[AnyRef]#RMIDispatcher, network: Network, invocation: LocalMethodInvocation[_]): Unit = {
        val args               = invocation.methodArguments
        val methodContract     = invocation.methodContract
        val behavior           = methodContract.behavior
        val parameterBehaviors = behavior.parameterBehaviors
        if (parameterBehaviors.isEmpty) {
            dispatcher.broadcast(args)
            return
        }
        val parameters          = invocation.methodDescription.params
        val parametersContracts = methodContract.parameterContracts
        val buff                = args.clone()
        dispatcher.foreachEngines(engineID => {
            val engine = network.findEngine(engineID).get
            for (i <- args.indices) {
                var finalRef = args(i)

                val param         = parameters(i)
                val paramTpe = param.getType.asInstanceOf[Class[_ >: AnyRef]]
                if (parametersContracts.nonEmpty) {
                    val paramContract = parametersContracts(i)

                    val modifier = paramContract.modifier.orNull

                    if (modifier != null) {
                        finalRef = modifier.toRemote(finalRef, invocation, engine)
                        modifier.toRemoteEvent(finalRef, invocation, engine)
                    }
                }
                finalRef = finalRef match {
                    case sync: SynchronizedObject[AnyRef] =>
                        val modifier = sync.getContract.modifier
                        if (modifier.isDefined)
                            modifier.get.modifyForParameter(sync, paramTpe)(invocation, engine, MethodCompModifierKind.TO_REMOTE)
                        else
                            sync
                    case x                                => x
                }
                buff(i) = finalRef
            }
            buff
        })
    }

}
