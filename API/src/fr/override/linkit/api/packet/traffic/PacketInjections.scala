package fr.`override`.linkit.api.packet.traffic

import java.util
import java.util.concurrent.ConcurrentHashMap

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

object PacketInjections {
    private val currentInjections = new ConcurrentHashMap[Int, util.Queue[(Int, Packet)]]

    private def factory = new util.PriorityQueue[(Int, Packet)]((o1: (Int, Packet), o2: (Int, Packet)) => Integer.compare(o1._1, o2._1))

    def createInjection(packet: Packet, coordinates: PacketCoordinates, number: Int): PacketInjection = this.synchronized {
        println(s"CREATED INJECTION NUMBER $number FOR PACKET $packet")
        val id = coordinates.injectableID
        var queue = currentInjections.get(id)
        if (queue == null) {
            queue = factory
            currentInjections.put(id, queue)
        }

        queue.add((number, packet))
        println(s"currentInjections WOWO = ${currentInjections}")
        println(s"queue for number $number = ${queue} (${System.identityHashCode(queue)})")
        new PacketInjection(coordinates)
    }

    def discovered(packet: Packet, coordinates: PacketCoordinates): PacketInjection = {
        val injection = new PacketInjection(coordinates)
        injection.discoveredPacket = packet
        injection
    }

    class PacketInjection private[PacketInjections](val coordinates: PacketCoordinates) {
        @volatile private[PacketInjections] var discoveredPacket: Packet = _

        def discoverPacket(): Packet = {
            val queue = currentInjections.get(coordinates.injectableID)
            val str = currentInjections.toString
            queue.synchronized {
                if (discoveredPacket != null)
                    return discoveredPacket

                println(s"currentInjections before = $str")
                val tuple = queue.poll()
                println(s"currentInjections after = $str")
                discoveredPacket = tuple._2
                discoveredPacket
            }
        }
    }

    override def toString: String = currentInjections.toString

}

