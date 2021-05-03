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

package fr.linkit.engine.connection.packet.traffic.channel.request

import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.local.concurrency.WorkerPool
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.packet.SimplePacketAttributes
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId

import scala.collection.mutable.ListBuffer

sealed abstract class Submitter[P](id: Int, scope: ChannelScope) extends SimplePacketAttributes {

    protected val packets: ListBuffer[Packet] = ListBuffer.empty[Packet]
    @volatile private var isSubmit            = false

    def addPacket(packet: Packet): this.type = {
        ensureNotSubmit()
        packets += packet
        this
    }

    def addPackets(packets: Packet*): this.type = {
        ensureNotSubmit()
        this.packets ++= packets
        this
    }

    def submit(): P = {
        ensureNotSubmit()
        AppLogger.vDebug(s"$currentTasksId <> Submitting ${getClass.getSimpleName} ($id)... with scope $scope")
        val result = makeSubmit()
        AppLogger.vDebug(s"$currentTasksId <> ${getClass.getSimpleName} ($id) submitted !")
        isSubmit = true
        result
    }

    protected def makeSubmit(): P

    private def ensureNotSubmit(): Unit = {
        if (isSubmit)
            throw new IllegalStateException("Response was already sent." + this)
    }

    override def toString: String = s"${getClass.getSimpleName}(id: $id, packets: $packets, isSubmit: $isSubmit)"

}

class ResponseSubmitter(id: Int, scope: ChannelScope) extends Submitter[Unit](id, scope) {

    override protected def makeSubmit(): Unit = {
        val response = ResponsePacket(id, packets.toArray)
        scope.sendToAll(response, SimplePacketAttributes(this))
    }
}

class RequestSubmitter(id: Int, scope: ChannelScope, pool: WorkerPool, handler: RequestPacketChannel) extends Submitter[RequestHolder](id, scope) {

    override protected def makeSubmit(): RequestHolder = {
        val holder  = RequestHolder(id, pool.newBusyQueue, handler)
        val request = RequestPacket(id, packets.toArray)

        handler.addRequestHolder(holder)
        scope.sendToAll(request, SimplePacketAttributes(this))

        holder
    }

}
