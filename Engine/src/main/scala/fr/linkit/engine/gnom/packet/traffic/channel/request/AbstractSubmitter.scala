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

    private var submitStackTrace: Exception = _

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

    override def submit(): P = this.synchronized {
        ensureNotSubmit()
        AppLoggers.GNOM.trace(s"Submitting ${getClass.getSimpleName} ($id)... with scope $scope")
        val result = makeSubmit()
        submitStackTrace = new Exception("stack trace")
        isSubmit = true
        result
    }

    protected def makeSubmit(): P

    private def ensureNotSubmit(): Unit = this.synchronized {
        if (isSubmit) {
            submitStackTrace.printStackTrace()
            throw new IllegalStateException("Response already set and submit !")
        }
    }

    override def toString: String = s"${getClass.getSimpleName}(id: $id, packets: $packets, isSubmit: $isSubmit)"

}

class ResponseSubmitter(id: Int, scope: ChannelScope) extends AbstractSubmitter[Unit](id, scope) with Submitter[Unit] {


    override protected def makeSubmit(): Unit = {
        val response = ResponsePacket(id, packets.toArray)
        scope.sendToAll(response, SimplePacketAttributes(this))
    }
}

class RequestSubmitter(id: Int, scope: ChannelScope, blockingQueue: BlockingQueue[AbstractSubmitterPacket], channel: SimpleRequestPacketChannel) extends AbstractSubmitter[ResponseHolder](id, scope) {

    override protected def makeSubmit(): SimpleResponseHolder = {

        val holder  = SimpleResponseHolder(id, blockingQueue, channel)
        val request = RequestPacket(id, packets.toArray)

        channel.addRequestHolder(holder)
        scope.sendToAll(request, SimplePacketAttributes(this))

        holder
    }

}
