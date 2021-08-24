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

import fr.linkit.api.connection.cache.obj._
import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehavior
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.invokation.remote.{DispatchableRemoteMethodInvocation, Puppeteer}
import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.connection.packet.channel.request.{RequestPacketChannel, ResponseHolder}
import fr.linkit.api.local.concurrency.ProcrastinatorControl
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.RMIExceptionString
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.local.utils.JavaUtils
import org.jetbrains.annotations.Nullable

class ObjectPuppeteer[S <: AnyRef](channel: RequestPacketChannel,
                                   procrastinator: ProcrastinatorControl,
                                   override val center: SynchronizedObjectCenter[_],
                                   override val nodeInfo: SyncNodeInfo,
                                   val wrapperBehavior: SynchronizedObjectBehavior[S]) extends Puppeteer[S] {

    private      val traffic                                         = channel.traffic
    override     val network                                         = traffic.connection.network
    override     val currentIdentifier: String                       = traffic.currentIdentifier
    private lazy val tree                                            = center.treeCenter.findTree(nodeInfo.nodePath.head).get
    private      val writer                                          = traffic.newWriter(channel.path)
    private var puppetWrapper         : S with SynchronizedObject[S] = _

    override def isCurrentEngineOwner: Boolean = ownerID == currentIdentifier

    override def getSynchronizedObject: S with SynchronizedObject[S] = puppetWrapper

    override def sendInvokeAndWaitResult[R](invocation: DispatchableRemoteMethodInvocation[R]): R = {
        val agreement = invocation.agreement
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("agreement may not perform remote invocation")
        val desiredEngineReturn = agreement.getDesiredEngineReturn

        if (desiredEngineReturn == currentIdentifier)
            throw new IllegalArgumentException("invocation's desired engine return is this engine.")

        val bhv      = invocation.methodBehavior
        val methodId = bhv.desc.methodId
        AppLogger.debug(s"Remotely invoking method ${bhv.desc.method.getName}")
        val scope            = new AgreementScope(writer, agreement)
        var requestResult: R = JavaUtils.nl()
        var isResultSet      = false
        val dispatcher       = new ObjectRMIDispatcher(scope, methodId, desiredEngineReturn) {
            override protected def handleResponseHolder(holder: ResponseHolder): Unit = {
                holder
                        .nextResponse
                        .nextPacket[RefPacket[R]].value match {
                    case RMIExceptionString(exceptionString) => throw new RemoteInvocationFailedException(s"Remote Invocation for method $methodId for engine id '$desiredEngineReturn' failed : $exceptionString")
                    case result                              =>
                        requestResult = result
                        isResultSet = true
                }
            }
        }
        invocation.dispatchRMI(dispatcher.asInstanceOf[Puppeteer[AnyRef]#RMIDispatcher])
        if (!isResultSet)
            throw new IllegalStateException("RMI dispatch has been processed asynchronously.")
        requestResult
    }

    override def sendInvoke(invocation: DispatchableRemoteMethodInvocation[_]): Unit = {
        val agreement = invocation.agreement
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("agreement may not perform remote invocation")

        procrastinator.runLater {
            val bhv      = invocation.methodBehavior
            val methodId = bhv.desc.methodId

            val scope      = new AgreementScope(writer, agreement)
            val dispatcher = new ObjectRMIDispatcher(scope, methodId, null)
            invocation.dispatchRMI(dispatcher.asInstanceOf[Puppeteer[AnyRef]#RMIDispatcher])
        }
    }

    override def init(wrapper: S with SynchronizedObject[S]): Unit = {
        if (this.puppetWrapper != null) {
            throw new IllegalStateException("This Puppeteer already controls a puppet instance !")
        }
        this.puppetWrapper = wrapper
    }

    override def synchronizedObj(obj: AnyRef, id: Int): AnyRef with SynchronizedObject[AnyRef] = {
        val currentPath = nodeInfo.nodePath
        tree.insertObject(currentPath, id, obj, currentIdentifier).synchronizedObject
    }

    class ObjectRMIDispatcher(scope: AgreementScope, methodID: Int, @Nullable returnEngine: String) extends RMIDispatcher {

        override def broadcast(args: Array[Any]): Unit = {
            handleResponseHolder(makeRequest(scope, args))
        }

        override def foreachEngines(action: String => Array[Any]): Unit = {
            scope.foreachAcceptedEngines(engineID => {
                if (engineID != returnEngine && engineID != currentIdentifier) //return engine is processed at last
                    makeRequest(ChannelScopes.include(engineID)(writer), action(engineID)).detach()
            })
            handleResponseHolder(makeRequest(ChannelScopes.include(returnEngine)(writer), action(returnEngine)))
        }

        private def makeRequest(scope: ChannelScope, args: Array[Any]): ResponseHolder = {
            channel.makeRequest(scope)
                    .addPacket(InvocationPacket(nodeInfo.nodePath, methodID, args, returnEngine))
                    .submit()
        }

        protected def handleResponseHolder(holder: ResponseHolder): Unit = holder.detach()
    }

}