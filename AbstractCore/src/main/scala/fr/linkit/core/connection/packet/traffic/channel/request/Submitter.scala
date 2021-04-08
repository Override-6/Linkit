package fr.linkit.core.connection.packet.traffic.channel.request

import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.ChannelScope
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId

import scala.collection.mutable.ListBuffer

sealed abstract class Submitter[P](id: Long, scope: ChannelScope) extends SimplePacketAttributes {

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

class ResponseSubmitter(id: Long, scope: ChannelScope) extends Submitter[Unit](id, scope) {

    override protected def makeSubmit(): Unit = {
        val response = ResponsePacket(id, packets.toArray)
        scope.sendToAll(response, SimplePacketAttributes(this))
    }
}

class RequestSubmitter(id: Long, scope: ChannelScope, pool: BusyWorkerPool, handler: RequestPacketChannel) extends Submitter[RequestHolder](id, scope) {

    override protected def makeSubmit(): RequestHolder = {
        val holder  = RequestHolder(id, pool.newBusyQueue, handler)
        val request = RequestPacket(id, packets.toArray)

        handler.addRequestHolder(holder)
        scope.sendToAll(request, SimplePacketAttributes(this))

        holder
    }

}
