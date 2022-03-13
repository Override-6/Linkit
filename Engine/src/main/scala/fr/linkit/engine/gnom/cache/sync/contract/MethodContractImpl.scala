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

package fr.linkit.engine.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreement
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contract.{MethodContract, ValueContract}
import fr.linkit.api.gnom.cache.sync.invocation.{HiddenMethodInvocationException, MirroringObjectInvocationException}
import fr.linkit.api.gnom.cache.sync.invocation.local.{CallableLocalMethodInvocation, LocalMethodInvocation}
import fr.linkit.api.gnom.cache.sync.invocation.remote.{DispatchableRemoteMethodInvocation, Puppeteer}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.invokation.AbstractMethodInvocation
import org.jetbrains.annotations.Nullable

class MethodContractImpl[R](forceLocalInnerInvocations: Boolean,
                            agreement: RMIRulesAgreement,
                            parameterContracts: Array[ValueContract[Any]],
                            returnValueContract: ValueContract[Any],
                            description: MethodDescription,
                            override val hideMessage: Option[String],
                            @Nullable override val procrastinator: Procrastinator) extends MethodContract[R] {

    override val isRMIActivated: Boolean = agreement.mayPerformRemoteInvocation

    override def synchronizeArguments(args: Array[Any], syncAction: AnyRef => SynchronizedObject[AnyRef]): Array[Any] = {
        if (parameterContracts.isEmpty)
            return args
        var i = -1
        args.map(param => {
            i += 1
            val contract = parameterContracts(i)
            param match {
                case sync: SynchronizedObject[_]               => sync
                case anyRef: AnyRef if contract.isSynchronized => syncAction(anyRef)
                case other                                     => other
            }
        })
    }

    override def handleInvocationResult(initialResult: Any, remote: Engine)(syncAction: AnyRef => SynchronizedObject[AnyRef]): Any = {
        var result = initialResult
        if (result != null && returnValueContract.isSynchronized && !result.isInstanceOf[SynchronizedObject[_]]) {
            val modifier = returnValueContract.modifier.orNull
            if (modifier != null)
                result = modifier.toRemote(result, remote)
            return syncAction(result.asInstanceOf[AnyRef])
        }
    }

    override def executeRemoteMethodInvocation(data: RemoteInvocationExecution): R = {
        val id                                                = description.methodId
        val node                                              = data.syncObject.getNode
        val args                                              = data.arguments
        val choreographer                                     = data.syncObject.getChoreographer
        val localInvocation: CallableLocalMethodInvocation[R] = new AbstractMethodInvocation[R](id, node) with CallableLocalMethodInvocation[R] {
            override val methodArguments: Array[Any] = args

            override def callSuper(): R = {
                if (forceLocalInnerInvocations) choreographer.forceLocalInvocation[R] {
                    data.doSuperCall().asInstanceOf[R]
                } else {
                    data.doSuperCall().asInstanceOf[R]
                }
            }
        }
        handleRMI(data.puppeteer, localInvocation)
    }

    override def executeMethodInvocation(origin: Engine, data: InvocationExecution): Unit = {
        val args = data.arguments
        val syncObject = data.syncObject
        if (hideMessage.isDefined)
            throw new HiddenMethodInvocationException(hideMessage.get)
        if (syncObject.isMirroring)
            throw new MirroringObjectInvocationException(s"Attempted to call a method on a distant object representation. This object is mirroring distant object ${syncObject.reference} on engine ${syncObject.origin}")
        modifyParameters(origin, args)
        AppLogger.debug {
            val name     = description.javaMethod.getName
            val methodID = description.methodId
            s"RMI - Calling method $methodID $name(${args.mkString(", ")})"
        }
        description.javaMethod.invoke(syncObject, args: _*)
    }

    @inline
    private def modifyParameters(engine: Engine, params: Array[Any]): Unit = {
        if (parameterContracts.isEmpty)
            return
        for (i <- params.indices) {
            val contract = parameterContracts(i)

            val modifier = contract.modifier.orNull
            var result   = params(i)
            if (modifier != null) {
                result = modifier.fromRemote(result, engine)
            }
            params(i) = result
        }
    }

    private def handleRMI(puppeteer: Puppeteer[_], localInvocation: CallableLocalMethodInvocation[R]): R = {
        // From here we are sure that we want to perform a remote
        // method invocation. (An invocation to the current machine (invocation.callSuper()) can be added).
        val currentIdentifier = puppeteer.currentIdentifier
        var result     : Any  = null
        var localResult: Any  = result
        val mayPerformRMI     = agreement.mayPerformRemoteInvocation
        if (agreement.mayCallSuper) {
            localResult = localInvocation.callSuper()
        }

        def syncValue(v: Any) = {
            if (v != null && returnValueContract.isSynchronized && !v.isInstanceOf[SynchronizedObject[AnyRef]]) {
                puppeteer.synchronizedObj(v.asInstanceOf[AnyRef])
            } else v
        }

        val currentMustReturn = agreement.getDesiredEngineReturn == currentIdentifier
        if (!mayPerformRMI) {
            return (if (currentMustReturn) syncValue(localResult) else null).asInstanceOf[R]
        }
        val remoteInvocation = new AbstractMethodInvocation[R](localInvocation) with DispatchableRemoteMethodInvocation[R] {
            override val agreement: RMIRulesAgreement = MethodContractImpl.this.agreement

            override def dispatchRMI(dispatcher: Puppeteer[AnyRef]#RMIDispatcher): Unit = {
                makeDispatch(puppeteer, dispatcher, localInvocation)
            }
        }
        if (currentMustReturn) {
            puppeteer.sendInvoke(remoteInvocation)
        } else {
            result = puppeteer.sendInvokeAndWaitResult(remoteInvocation)
        }

        syncValue(result).asInstanceOf[R]
    }

    private def makeDispatch(puppeteer: Puppeteer[_],
                             dispatcher: Puppeteer[AnyRef]#RMIDispatcher,
                             invocation: LocalMethodInvocation[_]): Unit = {
        val args    = invocation.methodArguments
        val network = puppeteer.network
        if (parameterContracts.isEmpty) {
            dispatcher.broadcast(args)
            return
        }
        val buff = args.clone()
        dispatcher.foreachEngines(engineID => {
            val engine = network.findEngine(engineID).get
            for (i <- args.indices) {
                var finalRef = args(i)
                if (finalRef != null) {
                    finalRef = modifyParam(i, finalRef)(engine, puppeteer)
                }
                buff(i) = finalRef
            }
            buff
        })
    }

    private def modifyParam(idx: Int, param: Any)(engine: Engine, puppeteer: Puppeteer[_]): Any = {
        var result = param
        if (parameterContracts.nonEmpty) {
            val paramContract = parameterContracts(idx)

            val modifier = paramContract.modifier.orNull

            if (modifier != null) {
                result = modifier.toRemote(result, engine)
                if (paramContract.isSynchronized) result match {
                    //The modification could return a non synchronized object even if the contract wants it to be synchronized.
                    case ref: AnyRef if !ref.isInstanceOf[SynchronizedObject[AnyRef]] =>
                        result = puppeteer.synchronizedObj(ref)

                    case _ =>
                }
            }
        }
        result
    }
}
