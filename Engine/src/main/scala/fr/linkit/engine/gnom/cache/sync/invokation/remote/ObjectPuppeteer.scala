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

package fr.linkit.engine.gnom.cache.sync.invokation.remote

import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.invokation.remote.{DispatchableRemoteMethodInvocation, Puppeteer}
import fr.linkit.api.gnom.cache.sync.tree.SyncObjectReference
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.request.{RequestPacketChannel, ResponseHolder}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.RMIExceptionString
import fr.linkit.engine.gnom.packet.fundamental.RefPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.internal.utils.JavaUtils
import org.jetbrains.annotations.Nullable

class ObjectPuppeteer[S <: AnyRef](channel: RequestPacketChannel,
                                   override val cache: SynchronizedObjectCache[_],
                                   override val nodeLocation: SyncObjectReference) extends Puppeteer[S] {

    private lazy val tree                       = cache.treeCenter.findTree(nodeLocation.nodePath.head).get
    override     val network          : Network = cache.network
    private      val traffic                    = channel.traffic
    override     val currentIdentifier: String  = traffic.currentIdentifier
    private      val writer                     = traffic.newWriter(channel.trafficPath)

    override def isCurrentEngineOwner: Boolean = ownerID == currentIdentifier

    override def sendInvokeAndWaitResult[R](invocation: DispatchableRemoteMethodInvocation[R]): R = {
        val agreement = invocation.agreement
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("agreement may not perform remote invocation")
        val desiredEngineReturn = agreement.getDesiredEngineReturn

        if (desiredEngineReturn == currentIdentifier)
            throw new IllegalArgumentException("invocation's desired engine return is this engine.")

        val contract = invocation.methodContract
        val desc     = contract.description
        val methodId = desc.methodId
        AppLogger.debug(s"Remotely invoking method ${desc.javaMethod.getName}")
        val scope            = new AgreementScope(writer, network, agreement)
        var requestResult: R = JavaUtils.nl()
        var isResultSet      = false
        val dispatcher       = new ObjectRMIDispatcher(scope, methodId, desiredEngineReturn) {
            override protected def handleResponseHolder(holder: ResponseHolder): Unit = {
                holder
                        .nextResponse
                        .nextPacket[RefPacket[R]].value match {
                    case RMIExceptionString(exceptionString) => throw new RemoteInvocationFailedException(s"Remote Invocation for method $methodId in engine of id '$desiredEngineReturn' failed : $exceptionString")
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

        val desc     = invocation.methodContract.description
        val methodId = desc.methodId

        val scope      = new AgreementScope(writer, network, agreement)
        val dispatcher = new ObjectRMIDispatcher(scope, methodId, null)
        invocation.dispatchRMI(dispatcher.asInstanceOf[Puppeteer[AnyRef]#RMIDispatcher])
    }

    override def synchronizedObj(obj: AnyRef, id: Int): AnyRef with SynchronizedObject[AnyRef] = {
        val currentPath = nodeLocation.nodePath
        tree.insertObject(currentPath, id, obj, currentIdentifier).synchronizedObject
    }

    class ObjectRMIDispatcher(scope: AgreementScope, methodID: Int, @Nullable returnEngine: String) extends RMIDispatcher {

        override def broadcast(args: Array[Any]): Unit = {
            handleResponseHolder(makeRequest(scope, args))
        }

        private def makeRequest(scope: ChannelScope, args: Array[Any]): ResponseHolder = {
            channel.makeRequest(scope)
                    .addPacket(InvocationPacket(nodeLocation.nodePath, methodID, args, returnEngine))
                    .submit()
        }

        protected def handleResponseHolder(holder: ResponseHolder): Unit = ()

        override def foreachEngines(action: String => Array[Any]): Unit = {
            scope.foreachAcceptedEngines(engineID => {
                //return engine is processed at last, don't send a request to the current engine
                if (engineID != returnEngine && engineID != currentIdentifier)
                    makeRequest(ChannelScopes.include(engineID)(writer), action(engineID))
            })
            if (returnEngine != null && returnEngine != currentIdentifier)
                handleResponseHolder(makeRequest(ChannelScopes.include(returnEngine)(writer), action(returnEngine)))
        }
    }

}