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

package fr.linkit.engine.gnom.packet.traffic.channel.request

import fr.linkit.api.gnom.packet.ChannelPacketBundle
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.channel.request.{RequestPacketBundle, RequestPacketChannel, ResponseHolder, Submitter}
import fr.linkit.api.gnom.packet.traffic.{PacketInjectableFactory, PacketInjectableStore}
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.AbstractPacketChannel
import fr.linkit.engine.internal.utils.ConsumerContainer

import java.lang.ref.{Reference, ReferenceQueue, WeakReference}
import java.util
import java.util.NoSuchElementException
import java.util.concurrent.LinkedBlockingQueue

class SimpleRequestPacketChannel(store: PacketInjectableStore, scope: ChannelScope) extends AbstractPacketChannel(store, scope) with RequestPacketChannel {

    private val requestHolders         = new util.LinkedHashMap[Int, WeakReference[SimpleResponseHolder]]()
    private val requestConsumers       = ConsumerContainer[DefaultRequestBundle]()
    @volatile private var requestCount = 0

    //debug only
    private val source = scope.traffic.currentIdentifier

    override def handleBundle(bundle: ChannelPacketBundle): Unit = {
        val packet = bundle.packet
        val attr   = bundle.attributes
        val coords = bundle.coords
        packet match {
            case request: RequestPacket =>
                AppLogger.vDebug(this)
                AppLogger.vDebug(s"${currentTasksId} <> $source: INJECTING REQUEST $request with attributes ${request.getAttributes}" + this)
                request.setAttributes(attr)

                val submitterScope = scope.shareWriter(ChannelScopes.include(coords.senderID))
                val submitter      = new ResponseSubmitter(request.id, submitterScope)

                requestConsumers.applyAllLater(DefaultRequestBundle(this, request, coords, submitter))

            case response: ResponsePacket =>
                AppLogger.vDebug(s"${currentTasksId} <> $source: INJECTING RESPONSE $response with attributes ${response.getAttributes}" + this)
                response.setAttributes(attr)

                val responseID = response.id
                Option(requestHolders.get(responseID)) match {
                    case Some(request)                     => request.get().pushResponse(response)
                    case None if responseID > requestCount =>
                        throw new NoSuchElementException(s"(${Thread.currentThread().getName}) Response.id not found (${response.id}) ($requestHolders)")
                    case _                                 =>
                }
        }
    }

    override def addRequestListener(callback: RequestPacketBundle => Unit): Unit = {
        requestConsumers += callback
    }

    override def makeRequest(scopeFactory: ScopeFactory[_ <: ChannelScope]): Submitter[ResponseHolder] = {
        makeRequest(scopeFactory(writer))
    }

    override def makeRequest(scope: ChannelScope): Submitter[ResponseHolder] = {
        if (!(scope.writer.path sameElements trafficPath))
            throw new IllegalArgumentException("Scope is not set on the same injectable id of this packet channel.")
        val requestID = nextRequestID
        //TODO Make an adaptive queue that make non WorkerPool threads wait and worker pools change task when polling.
        val queue     = WorkerPools.currentPool.map(_.newBusyQueue[AbstractSubmitterPacket]).getOrElse(new LinkedBlockingQueue[AbstractSubmitterPacket]())
        new RequestSubmitter(requestID, scope, queue, this)
    }

    private def nextRequestID: Int = {
        requestCount += 1
        requestCount
    }

    private[request] def addRequestHolder(holder: SimpleResponseHolder): Unit = {
        requestHolders.put(holder.id, new WeakReference[SimpleResponseHolder](holder, eventQueue))
    }

    private val eventQueue: ReferenceQueue[SimpleResponseHolder] = new ReferenceQueue[SimpleResponseHolder]() {
        override def poll(): Reference[SimpleResponseHolder] = {
            requestHolders.remove(super.poll().get().id)
        }
    }

}

object SimpleRequestPacketChannel extends PacketInjectableFactory[SimpleRequestPacketChannel] {

    override def createNew(store: PacketInjectableStore, scope: ChannelScope): SimpleRequestPacketChannel = new SimpleRequestPacketChannel(store, scope)
}
