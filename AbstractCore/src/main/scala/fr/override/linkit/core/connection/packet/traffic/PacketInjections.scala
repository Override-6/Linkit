package fr.`override`.linkit.core.connection.packet.traffic

import java.util.concurrent.ConcurrentHashMap

import fr.`override`.linkit.api.connection.packet.traffic.PacketInjection
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}

object PacketInjections {
    private[traffic] val currentInjections = new ConcurrentHashMap[(Int, String), DirectInjection]

    def createInjection(packet: Packet, coordinates: DedicatedPacketCoordinates, number: Int): PacketInjection = this.synchronized {
        //println(s"CREATING INJECTION FOR PACKET $packet WITH COORDINATES $coordinates, $number")
        val id = coordinates.injectableID
        val sender = coordinates.senderID

        var injection = currentInjections.get((id, sender))
        if (injection == null) {
            injection = new DirectInjection(coordinates)
            currentInjections.put((id, sender), injection)
        }

        injection.addPacket(number, packet)
        injection
    }

    def unhandled(coordinates: DedicatedPacketCoordinates, packets: Packet*): PacketInjection = {
        //println(s"NEW UNHANDLED FOR COORDINATES $coordinates / PACKETS ${packets}")
        val injection = new DirectInjection(coordinates)
        var i = 0
        packets.foreach(packet => {
            i += 1
            injection.addPacket(i, packet)
        })
        injection
    }

}

