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

import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreement
import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncClassDef}
import fr.linkit.api.gnom.cache.sync.contract.{MethodContract, ModifiableValueContract, SyncLevel}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod._
import fr.linkit.api.gnom.cache.sync.invocation.local.{CallableLocalMethodInvocation, LocalMethodInvocation}
import fr.linkit.api.gnom.cache.sync.invocation.remote.{DispatchableRemoteMethodInvocation, Puppeteer}
import fr.linkit.api.gnom.cache.sync.invocation.{HiddenMethodInvocationException, InvocationChoreographer, InvocationHandlingMethod, MirroringObjectInvocationException}
import fr.linkit.api.gnom.cache.sync.tree.ObjectConnector
import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.invokation.AbstractMethodInvocation
import fr.linkit.engine.internal.manipulation.invokation.MethodInvoker
import org.jetbrains.annotations.Nullable

import scala.annotation.switch

class MethodContractImpl[R](override val invocationHandlingMethod: InvocationHandlingMethod,
                            parentChoreographer: InvocationChoreographer,
                            agreement: RMIRulesAgreement,
                            parameterContracts: Array[ModifiableValueContract[Any]],
                            returnValueContract: ModifiableValueContract[Any],
                            override val description: MethodDescription,
                            override val hideMessage: Option[String],
                            @Nullable override val procrastinator: Procrastinator) extends MethodContract[R] {
    
    override val isRMIActivated: Boolean = agreement.mayPerformRemoteInvocation
    
    override val choreographer: InvocationChoreographer = new InvocationChoreographer(parentChoreographer)
    
    override def connectArgs(args: Array[Any], syncAction: (Any, SyncLevel) => ConnectedObject[AnyRef]): Unit = {
        if (parameterContracts.isEmpty)
            return
        var i = 0
        while (i < args.length) {
            val contract = parameterContracts(i)
            args(i) = {
                val obj = args(i)
                if (contract.registrationKind != NotRegistered)
                    syncObj(obj, contract.registrationKind, syncAction)
                else obj
            }
            i += 1
        }
    }
    
    private def syncObj(obj: Any, level: SyncLevel, syncAction: (Any, SyncLevel) => ConnectedObject[AnyRef]): Any = {
        if (obj.isInstanceOf[ConnectedObject[_]]) return obj //object is already connected
        if ((level == SyncLevel.Chipped) || SyncClassDef(obj.getClass).isOverrideable)
            syncAction(obj.asInstanceOf[AnyRef], level).connected
        else if (level.isConnectable && returnValueContract.autoChip)
            syncAction(obj.asInstanceOf[AnyRef], Chipped).connected
        else
            throw new CannotConnectException(s"Attempted to connect object '$obj', but object's class (${obj.getClass}) is not overrideable.")
    }
    
    override def applyReturnValue(rv: Any, syncAction: (Any, SyncLevel) => ConnectedObject[AnyRef]): Any = {
        if (rv != null && returnValueContract.registrationKind != NotRegistered) syncObj(rv, returnValueContract.registrationKind, syncAction)
        else rv
    }
    
    override def handleInvocationResult(initialResult: Any, remote: Engine)(syncAction: (Any, SyncLevel) => ConnectedObject[AnyRef]): Any = {
        if (initialResult == null) return null
        var result = initialResult
        val kind   = returnValueContract.registrationKind
        if (kind != NotRegistered && !result.isInstanceOf[ConnectedObject[_]]) {
            val modifier = returnValueContract.modifier.orNull
            if (modifier != null)
                result = modifier.toRemote(result, remote)
            return {
                if (kind.isConnectable)
                    syncObj(result, kind, syncAction)
                else result
            }
        }
        result
    }
    
    override def executeRemoteMethodInvocation(data: RemoteInvocationExecution): R = {
        val id                                                = description.methodId
        val node                                              = data.obj.getNode
        val args                                              = data.arguments
        val choreographer                                     = data.obj.getChoreographer
        val localInvocation: CallableLocalMethodInvocation[R] = new AbstractMethodInvocation[R](id, node, data.connector) with CallableLocalMethodInvocation[R] {
            override val methodArguments: Array[Any] = args
            
            override def callSuper(): R = {
                import choreographer._
                (invocationHandlingMethod: @switch) match {
                    case DisableSubInvocations => disinv(data.doSuperCall().asInstanceOf[R])
                    case EnableSubInvocations  => ensinv(data.doSuperCall().asInstanceOf[R])
                    case Inherit               => data.doSuperCall().asInstanceOf[R]
                }
            }
        }
        handleRMI(data.puppeteer, localInvocation)
    }
    
    override def executeMethodInvocation(origin: Engine, data: InvocationExecution): Any = {
        val args = data.arguments
        val obj  = data.obj
        if (hideMessage.isDefined)
            throw new HiddenMethodInvocationException(hideMessage.get)
        if (isMirroring(obj))
            throw new MirroringObjectInvocationException(s"Attempted to call a method on a distant object representation. This object is mirroring distant object ${obj.reference} on engine ${obj.ownerID}")
        modifyArgsIn(origin, args)
        val method = description.javaMethod
        AppLoggers.SyncObj.debug {
            val name        = method.getName
            val methodID    = description.methodId
            val methodClass = obj.getClassDef.mainClass.getName
            s"Calling method $methodID $methodClass.$name(${args.map(_.getClass.getSimpleName).mkString(", ")}) (on object: ${obj.reference})"
        }
        val target = obj.connected
        if (description.isMethodAccessible)
            method.invoke(target, args: _*)
        else {
            //TODO can be optimised (cache the MethodInvoker)
            new MethodInvoker(method).invoke(target, args)
        }
    }
    
    private def isMirroring(obj: ChippedObject[_]): Boolean = obj match {
        case s: SynchronizedObject[_] => s.isMirroring
        case _                        => false
    }
    
    private def modifyArgsIn(engine: Engine, params: Array[Any]): Unit = {
        val pcLength = parameterContracts.length
        if (pcLength == 0)
            return
        if (params.length != pcLength)
            throw new IllegalArgumentException("Params array length is not equals to parameter contracts length.")
        for (i <- 0 until pcLength) {
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
        
        val currentIdentifier = puppeteer.currentIdentifier
        val objectNode        = localInvocation.objectNode
        val obj               = objectNode.obj
        val mayPerformRMI     = agreement.mayPerformRemoteInvocation
        val connector         = localInvocation.connector
        
        var result     : Any = null
        var localResult: Any = null
        if (agreement.mayCallSuper && !isMirroring(obj)) {
            localResult = localInvocation.callSuper()
        }
        
        def syncValue(v: Any) = {
            val level = returnValueContract.registrationKind
            if (v != null && level != NotRegistered && !v.isInstanceOf[ConnectedObject[AnyRef]]) {
                syncObj(v, level, connector.createConnectedObj(obj.reference)(_, _))
            } else v
        }
        
        val currentMustReturn = agreement.getAppointedEngineReturn == currentIdentifier
        if (!mayPerformRMI) {
            return (if (currentMustReturn) syncValue(localResult) else null).asInstanceOf[R]
        }
        
        val remoteInvocation = new AbstractMethodInvocation[R](localInvocation) with DispatchableRemoteMethodInvocation[R] {
            override val agreement: RMIRulesAgreement = MethodContractImpl.this.agreement
            
            override def dispatchRMI(dispatcher: Puppeteer[AnyRef]#RMIDispatcher): Unit = {
                makeDispatch(puppeteer.network, dispatcher, localInvocation)
            }
        }
        if (currentMustReturn) {
            AppLoggers.SyncObj.debug(debugMsg(null, localInvocation))
            puppeteer.sendInvoke(remoteInvocation)
            result = localResult
        } else {
            AppLoggers.SyncObj.debug(debugMsg(remoteInvocation.agreement.getAppointedEngineReturn, localInvocation))
            result = puppeteer.sendInvokeAndWaitResult(remoteInvocation)
        }
        
        syncValue(result).asInstanceOf[R]
    }
    
    private def debugMsg(appointed: String, invocation: LocalMethodInvocation[_]): String = {
        if (!AppLoggers.SyncObj.isDebugEnabled) return null
        val method               = description.javaMethod
        val name                 = method.getName
        val methodID             = description.methodId
        val obj                  = invocation.objectNode.obj
        val className            = obj.getClassDef.mainClass.getName
        val params               = invocation.methodArguments.mkString(", ")
        val expectedEngineReturn = if (appointed == null) " - no remote return value is expected, the RMI is performed asynchronously." else " - expected return value from " + appointed + "."
        s"Sending method invocation ($methodID) $className.$name($params) (on object: ${obj.reference}) " + expectedEngineReturn
    }
    
    private def makeDispatch(network: Network,
                             dispatcher: Puppeteer[AnyRef]#RMIDispatcher,
                             invocation: LocalMethodInvocation[_]): Unit = {
        val args = invocation.methodArguments
        if (parameterContracts.isEmpty) {
            dispatcher.broadcast(args)
            return
        }
        val buff = args.clone()
        dispatcher.foreachEngines(engineID => {
            val target = network.findEngine(engineID).get
            modifyArgsOut(buff, target, invocation.objectNode.reference, invocation.connector)
            buff
        })
    }
    
    private def modifyArgsOut(args: Array[Any], target: Engine, objRef: ConnectedObjectReference, connector: ObjectConnector): Unit = {
        def modifyParamOut(idx: Int, param: Any): Any = {
            var result = param
            if (parameterContracts.nonEmpty) {
                val paramContract = parameterContracts(idx)
                
                val modifier = paramContract.modifier.orNull
                
                if (modifier != null) {
                    result = modifier.toRemote(result, target)
                    if (paramContract.registrationKind != NotRegistered) result match {
                        //The modification could return a non synchronized object even if the contract wants it to be synchronized.
                        case ref: AnyRef if !ref.isInstanceOf[ConnectedObject[AnyRef]] =>
                            result = connector.createConnectedObj(objRef)(ref, paramContract.registrationKind).connected
                        case _                                                         =>
                    }
                }
            }
            result
        }
        
        for (i <- args.indices) {
            var finalRef = args(i)
            if (finalRef != null) {
                finalRef = modifyParamOut(i, finalRef)
            }
            args(i) = finalRef
        }
    }
    
    override def toString: String = s"method contract ${description.javaMethod}"
    
}
