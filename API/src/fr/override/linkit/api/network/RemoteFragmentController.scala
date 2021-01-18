package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.packet.channel.AsyncPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class RemoteFragmentController(val nameIdentifier: String, channel: AsyncPacketChannel) {

    private val listeners = ConsumerContainer[(Packet, PacketCoordinates)]()

    channel.addOnPacketInjected((packet, coords) => {
        packet match {
            case WrappedPacket(this.nameIdentifier, subPacket) => listeners.applyAll((subPacket, coords))
        }
    })

    def addOnPacketReceived(callback: (Packet, PacketCoordinates) => Unit): Unit = {
        listeners += (tuple2 => callback(tuple2._1, tuple2._2))
    }

    def send(packet: Packet): Unit = {
        channel.sendPacket(WrappedPacket(nameIdentifier, packet))
    }

    override def toString: String = s"RemoteFragmentController($nameIdentifier)"

}
