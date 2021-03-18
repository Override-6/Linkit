package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.api.connection.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.connection.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.internal.utils.ConsumerContainer

class RemoteFragmentController(val nameIdentifier: String, val channel: CommunicationPacketChannel) {

    private val listeners = ConsumerContainer[(Packet, PacketCoordinates)]()

    channel.addRequestListener((packet, coords) => {
        packet match {
            case WrappedPacket(this.nameIdentifier, subPacket) => listeners.applyAll((subPacket, coords))
            case _ =>
        }
    })

    def addOnRequestReceived(callback: (Packet, PacketCoordinates) => Unit): Unit = {
        listeners += (tuple2 => callback(tuple2._1, tuple2._2))
    }

    def sendRequest(packet: Packet): Unit = {
        channel.sendRequest(WrappedPacket(nameIdentifier, packet))
    }

    def sendResponse(packet: Packet): Unit = {
        channel.sendResponse(packet)
    }

    def nextResponse[P <: Packet]: P = channel.nextResponse.asInstanceOf[P]

    override def toString: String = s"RemoteFragmentController($nameIdentifier)"

}
