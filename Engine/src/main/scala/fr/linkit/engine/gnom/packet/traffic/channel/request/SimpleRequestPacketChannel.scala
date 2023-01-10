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

package fr.linkit.engine.gnom.packet.traffic.channel.request

import fr.linkit.api.gnom.packet.ChannelPacketBundle
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.channel.request.{RequestPacketBundle, RequestPacketChannel, ResponseHolder, Submitter}
import fr.linkit.api.gnom.packet.traffic.PacketInjectableFactory
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.AbstractPacketChannel
import fr.linkit.engine.internal.util.ConsumerContainer

import java.util
import java.util.concurrent.LinkedBlockingQueue

class SimpleRequestPacketChannel(scope: ChannelScope) extends AbstractPacketChannel(scope) with RequestPacketChannel {

    private val requestHolders         = new util.LinkedHashMap[Int, SimpleResponseHolder]()
    private val requestConsumers       = ConsumerContainer[DefaultRequestPacketChannelBundle]()
    @volatile private var requestCount = 0

    override def handleBundle(bundle: ChannelPacketBundle): Unit = {
        val packet = bundle.packet
        val attr   = bundle.attributes
        val coords = bundle.coords
        packet match {
            case request: RequestPacket =>
                request.setAttributes(attr)

                val submitterScope = scope.shareWriter(ChannelScopes.apply(coords.senderTag))
                val submitter      = new ResponseSubmitter(request.id, submitterScope)
                val channelBundle  = DefaultRequestPacketChannelBundle(this, request, coords, submitter, bundle.ordinal)
                Procrastinator.current match {
                    case Some(_) =>
                        requestConsumers.applyAll(channelBundle)
                    case None    =>
                        requestConsumers.applyAll(channelBundle)
                }

            case response: ResponsePacket =>
                response.setAttributes(attr)

                val responseID = response.id
                Option(requestHolders.get(responseID)) match {
                    case Some(request)                     =>
                        request.pushResponse(response)
                    case None if responseID > requestCount =>
                        throw new NoSuchElementException(s"(${Thread.currentThread().getName}) Response.id not found (${response.id}) ($requestHolders)")
                    case None                              =>
                        requestHolders remove responseID
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
        val queue     = new LinkedBlockingQueue[AbstractSubmitterPacket]()
        new RequestSubmitter(requestID, scope, queue, this)
    }

    private def nextRequestID: Int = {
        requestCount += 1
        requestCount
    }

    private[request] def addRequestHolder(holder: SimpleResponseHolder): Unit = {
        requestHolders.put(holder.id, holder)
    }

}

object SimpleRequestPacketChannel extends PacketInjectableFactory[SimpleRequestPacketChannel] {

    override def createNew(scope: ChannelScope): SimpleRequestPacketChannel = new SimpleRequestPacketChannel(scope)
}
