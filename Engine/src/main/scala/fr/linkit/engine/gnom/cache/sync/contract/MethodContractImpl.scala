/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIDispatchAgreement
import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncClassDef}
import fr.linkit.api.gnom.cache.sync.contract.{MethodContract, SyncLevel, ValueContract}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod._
import fr.linkit.api.gnom.cache.sync.invocation.local.{CallableLocalMethodInvocation, LocalMethodInvocation}
import fr.linkit.api.gnom.cache.sync.invocation.remote.{DispatchableRemoteMethodInvocation, Puppeteer}
import fr.linkit.api.gnom.cache.sync.invocation.{HiddenMethodInvocationException, InvocationChoreographer, InvocationHandlingMethod, MirroringObjectInvocationException}
import fr.linkit.api.gnom.network.tag.{Current, NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.invokation.AbstractMethodInvocation
import fr.linkit.engine.gnom.network.NetworkDataTrunk
import fr.linkit.engine.internal.debug.{Debugger, MethodInvocationSendStep}
import fr.linkit.engine.internal.manipulation.invokation.MethodInvoker
import org.jetbrains.annotations.Nullable

import scala.annotation.switch

class MethodContractImpl[R](override val invocationHandlingMethod: InvocationHandlingMethod,
                            parentChoreographer                  : InvocationChoreographer,
                            agreement                            : RMIDispatchAgreement,
                            parameterContracts                   : Array[ValueContract],
                            returnValueContract                  : ValueContract,
                            override val description             : MethodDescription,
                            override val hideMessage             : Option[String],
                            @Nullable override val procrastinator: Procrastinator) extends MethodContract[R] {

    override def isRMIActivated: Boolean = agreement.mayPerformRemoteInvocation

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

    override def handleInvocationResult(initialResult: Any)(syncAction: (Any, SyncLevel) => ConnectedObject[AnyRef]): Any = {
        if (initialResult == null) return null
        var result = initialResult
        val kind   = returnValueContract.registrationKind
        if (kind != NotRegistered && !result.isInstanceOf[ConnectedObject[_]]) {
            if (kind.isConnectable)
                result = syncObj(result, kind, syncAction)
        }
        result
    }

    override def executeRemoteMethodInvocation(data: RemoteInvocationExecution): R = {
        val id                                                = description.methodId
        val node                                              = data.obj.getCompanion
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
        handleInvocation(data.puppeteer, localInvocation)
    }

    override def executeMethodInvocation(data: InvocationExecution): Any =  {
        val args = data.arguments
        val obj  = data.obj
        if (hideMessage.isDefined)
            throw new HiddenMethodInvocationException(hideMessage.get)
        if (isMirroring(obj))
            throw new MirroringObjectInvocationException(s"Attempted to call a method on a distant object representation. This object is mirroring distant object ${obj.reference} on engine ${obj.owner}")
        val method = description.javaMethod
        AppLoggers.COInv.debug {
            val name        = method.getName
            val methodID    = description.methodId
            val methodClass = obj.getClassDef.mainClass.getName
            val argsString  = args.map(i => if (i == null) "null" else i.getClass.getSimpleName).mkString(", ")
            s"Calling method $methodID $methodClass.$name($argsString) (on object: ${obj.reference})"
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

    private val isContractOfNetworkTrunk = description.javaMethod.getDeclaringClass == classOf[NetworkDataTrunk]
    def mayCallSuper: Boolean = {
        isContractOfNetworkTrunk || agreement.mayCallSuper
    }

    private def handleInvocation(puppeteer: Puppeteer[_], invocation: CallableLocalMethodInvocation[R]): R = {
        val objectNode    = invocation.objectNode
        val obj           = objectNode.obj
        val mayPerformRMI = agreement.mayPerformRemoteInvocation
        val connector     = invocation.connector

        var result     : Any = null
        var localResult: Any = null
        if (mayCallSuper && !isMirroring(obj)) {
            localResult = invocation.callSuper()
        }

        def syncValue(v: Any) = {
            val level = returnValueContract.registrationKind
            if (v != null && level != NotRegistered && !v.isInstanceOf[ConnectedObject[AnyRef]]) {
                syncObj(v, level, connector.createConnectedObj(obj.reference)(_, _))
            } else v
        }

        val appointedEngine   = agreement.getAppointedEngineReturn
        val currentMustReturn = agreement.currentMustReturn
        if (!mayPerformRMI) {
            return (if (currentMustReturn) syncValue(localResult) else null).asInstanceOf[R]
        }

        val remoteInvocation = new AbstractMethodInvocation[R](invocation) with DispatchableRemoteMethodInvocation[R] {
            override val agreement: RMIDispatchAgreement = MethodContractImpl.this.agreement

            override def dispatchRMI(dispatcher: Puppeteer[AnyRef]#RMIDispatcher): Unit = {
                val args = invocation.methodArguments
                dispatcher.sendToSelection(args)
            }
        }
        if (currentMustReturn) {
            Debugger.push(MethodInvocationSendStep(agreement, description, null))
            AppLoggers.COInv.debug(debugMsg(Current, invocation))
            puppeteer.sendInvoke(remoteInvocation)
            result = localResult
            Debugger.pop()
        } else {
            Debugger.push(MethodInvocationSendStep(agreement, description, appointedEngine))
            AppLoggers.COInv.debug(debugMsg(appointedEngine, invocation))
            result = puppeteer.sendInvokeAndWaitResult(remoteInvocation)
            Debugger.pop()
        }
        syncValue(result).asInstanceOf[R]
    }

    private def debugMsg(appointed: UniqueTag with NetworkFriendlyEngineTag, invocation: LocalMethodInvocation[_]): String = {
        if (!AppLoggers.COInv.isDebugEnabled) return null
        val method    = description.javaMethod
        val name      = method.getName
        val methodID  = description.methodId
        val obj       = invocation.objectNode.obj
        val className = obj.getClassDef.mainClass.getName
        val params    = invocation.methodArguments.map(i => if (i == null) "null" else i.getClass.getSimpleName).mkString(", ")

        val expectedEngineReturn = {
            if (appointed == null) " - no remote return value is expected, the RMI is performed asynchronously."
            else " - expected return value from " + appointed + "."
        }
        s"Sending method invocation ($methodID) $className.$name($params) (on object: ${obj.reference}) " + expectedEngineReturn
    }
    override def toString: String = s"method contract ${description.javaMethod}"

}
