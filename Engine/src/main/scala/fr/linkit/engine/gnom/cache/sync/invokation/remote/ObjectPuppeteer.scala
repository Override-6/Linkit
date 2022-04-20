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

import fr.linkit.api.gnom.cache.sync.invocation.InvocationFailedException
import fr.linkit.api.gnom.cache.sync.invocation.remote.{DispatchableRemoteMethodInvocation, Puppeteer}
import fr.linkit.api.gnom.cache.sync.{SyncObjectReference, _}
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.request.{RequestPacketChannel, ResponseHolder}
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.RMIExceptionString
import fr.linkit.engine.gnom.cache.sync.tree.DefaultSyncObjectForest
import fr.linkit.engine.gnom.packet.fundamental.RefPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.internal.utils.JavaUtils
import org.jetbrains.annotations.Nullable

class ObjectPuppeteer[S <: AnyRef](channel: RequestPacketChannel,
                                   override val cache: SynchronizedObjectCache[_],
                                   override val nodeReference: SyncObjectReference) extends Puppeteer[S] {

    private lazy val tree                       = cache.forest.findTree(nodeReference.nodePath.head).get
    override     val network          : Network = cache.network
    private      val traffic                    = channel.traffic
    override     val currentIdentifier: String  = traffic.currentIdentifier
    private      val writer                     = traffic.newWriter(channel.trafficPath)
    private      val channelNode                = traffic.findNode(channel.trafficPath).get

    override def isCurrentEngineOwner: Boolean = ownerID == currentIdentifier

    private def isPerformant = channelNode.preferPerformances()

    override def sendInvokeAndWaitResult[R](invocation: DispatchableRemoteMethodInvocation[R]): R = {
        val agreement = invocation.agreement
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("the agreement states that the method should not be called on a remote engine")
        val desiredEngineReturn = agreement.getAppointedEngineReturn

        if (desiredEngineReturn == currentIdentifier)
            throw new UnsupportedOperationException("invocation's desired engine return is this engine.")

        val methodId         = invocation.methodID
        val scope            = new AgreementScope(writer, network, agreement)
        var requestResult: R = JavaUtils.nl()
        var isResultSet      = false
        val dispatcher       = new ObjectRMIDispatcher(scope, methodId, desiredEngineReturn) {
            override protected def handleResponseHolder(holder: ResponseHolder): Unit = {
                holder
                        .nextResponse
                        .nextPacket[Packet] match {
                    case RMIExceptionString(exceptionString) =>
                        throw new InvocationFailedException(s"Remote Method Invocation for method with id $methodId on object $nodeReference, executed by engine '$desiredEngineReturn' failed :\n $exceptionString")
                    case p: RefPacket[R]                     =>
                        requestResult = p.value
                        isResultSet = true
                }
            }
        }
        invocation.dispatchRMI(dispatcher.asInstanceOf[Puppeteer[AnyRef]#RMIDispatcher])
        if (!isResultSet)
            throw new IllegalStateException("RMI dispatch has been processed asynchronously.")
        requestResult match {
            case r: R with AnyRef =>
                if (cache.forest.asInstanceOf[DefaultSyncObjectForest[AnyRef]].isObjectLinked(r))
                    tree.insertObject(nodeReference.nodePath, r, agreement.getAppointedEngineReturn).synchronizedObject
                else r
        }
    }

    override def sendInvoke(invocation: DispatchableRemoteMethodInvocation[_]): Unit = {
        val agreement = invocation.agreement
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("agreement may not perform remote invocation")

        val methodId = invocation.methodID

        runLaterIfPerformantChannelElseRun {
            val scope      = new AgreementScope(writer, network, agreement)
            val dispatcher = new ObjectRMIDispatcher(scope, methodId, null)
            invocation.dispatchRMI(dispatcher.asInstanceOf[Puppeteer[AnyRef]#RMIDispatcher])
        }
    }

    override def synchronizedObj(obj: Any): SynchronizedObject[AnyRef] = {
        val currentPath = nodeReference.nodePath
        tree.insertObject(currentPath, obj.asInstanceOf[AnyRef], currentIdentifier).synchronizedObject
    }

    class ObjectRMIDispatcher(scope: AgreementScope, methodID: Int, @Nullable returnEngine: String) extends RMIDispatcher {

        override def broadcast(args: Array[Any]): Unit = {
            handleResponseHolder(makeRequest(scope, args))
        }

        private def makeRequest(scope: ChannelScope, args: Array[Any]): ResponseHolder = {
            channel.makeRequest(scope)
                    .addPacket(InvocationPacket(nodeReference.nodePath, methodID, args, returnEngine))
                    .submit()
        }

        protected def handleResponseHolder(holder: ResponseHolder): Unit = ()

        override def foreachEngines(action: String => Array[Any]): Unit = {
            scope.foreachAcceptedEngines(engineID => runLaterIfPerformantChannelElseRun {
                //return engine is processed at last, don't send a request to the current engine
                if (engineID != returnEngine && engineID != currentIdentifier)
                    makeRequest(ChannelScopes.include(engineID)(writer), action(engineID))
            })
            if (returnEngine != null && returnEngine != currentIdentifier)
                handleResponseHolder(makeRequest(ChannelScopes.include(returnEngine)(writer), action(returnEngine)))
        }
    }

    private val procrastinator: Procrastinator = network.connection

    private def runLaterIfPerformantChannelElseRun(action: => Unit): Unit = {
        if (isPerformant) procrastinator.runLater(action)
        else action
    }

}

object ObjectPuppeteer {

    def apply[S <: AnyRef](channel: RequestPacketChannel, cache: SynchronizedObjectCache[_], nodeLocation: SyncObjectReference): ObjectPuppeteer[S] = new ObjectPuppeteer(channel, cache, nodeLocation)
}