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

import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.request.{ResponseHolder, Submitter}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.SimplePacketAttributes

import java.util.concurrent.BlockingQueue
import scala.collection.mutable.ListBuffer

sealed abstract class AbstractSubmitter[P](id: Int, scope: ChannelScope) extends SimplePacketAttributes with Submitter[P] {

    protected val packets: ListBuffer[Packet] = ListBuffer.empty[Packet]
    @volatile private var isSubmit            = false

    override def addPacket(packet: Packet): this.type = {
        ensureNotSubmit()
        packets += packet
        this
    }

    override def addPackets(packets: Packet*): this.type = {
        ensureNotSubmit()
        this.packets ++= packets
        this
    }

    override def submit(): P = {
        ensureNotSubmit()
        AppLoggers.GNOM.trace(s"Submitting ${getClass.getSimpleName} ($id)... with scope $scope")
        val result = makeSubmit()
        isSubmit = true
        result
    }

    protected def makeSubmit(): P

    private def ensureNotSubmit(): Unit = {
        if (isSubmit)
            throw new IllegalStateException("Response was already sent " + this)
    }

    override def toString: String = s"${getClass.getSimpleName}(id: $id, packets: $packets, isSubmit: $isSubmit)"

}

class ResponseSubmitter(id: Int, scope: ChannelScope) extends AbstractSubmitter[Unit](id, scope) with Submitter[Unit] {

    override protected def makeSubmit(): Unit = {
        val response = ResponsePacket(id, packets.toArray)
        scope.sendToAll(response, SimplePacketAttributes(this))
    }
}

class RequestSubmitter(id: Int, scope: ChannelScope, blockingQueue: BlockingQueue[AbstractSubmitterPacket], handler: SimpleRequestPacketChannel) extends AbstractSubmitter[ResponseHolder](id, scope) {

    override protected def makeSubmit(): SimpleResponseHolder = {
        val holder  = SimpleResponseHolder(id, blockingQueue, handler)
        val request = RequestPacket(id, packets.toArray)

        handler.addRequestHolder(holder)
        scope.sendToAll(request, SimplePacketAttributes(this))

        holder
    }

}
