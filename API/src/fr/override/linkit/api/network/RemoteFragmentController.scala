package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.packet.traffic.dedicated.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketCoordinates}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

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

    def nextResponse[P <: Packet](factory: PacketCompanion[P]): P = nextResponse().asInstanceOf[P]

    def nextResponse(): Packet = {
        channel.nextResponse()
    }

    override def toString: String = s"RemoteFragmentController($nameIdentifier)"

}
