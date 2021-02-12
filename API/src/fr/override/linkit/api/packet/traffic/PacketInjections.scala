package fr.`override`.linkit.api.packet.traffic

import java.util.concurrent.ConcurrentHashMap

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

import scala.collection.mutable.ListBuffer

object PacketInjections {
    private val currentInjections = new ConcurrentHashMap[Int, PacketInjection]

    def createInjection(packet: Packet, coordinates: PacketCoordinates, number: Int): PacketInjection = this.synchronized {
        val id = coordinates.injectableID
        var injection = currentInjections.get(id)
        if (injection == null) {
            injection = new PacketInjection(coordinates)
            currentInjections.put(id, injection)
        }

        injection.injections += ((number, packet))
        injection
    }

    def unhandled(coordinates: PacketCoordinates, packets: Packet*): PacketInjection = {
        val injection = new PacketInjection(coordinates)
        var i = 0
        packets.foreach(packet => {
            i += 1
            injection.injections += ((i, packet))
        })
        injection
    }

    class PacketInjection private[PacketInjections](val coordinates: PacketCoordinates) {

        private[PacketInjections] val injections = ListBuffer.empty[(Int, Packet)]
        private val injectableID = coordinates.injectableID
        //The thread that created this object
        //will be set as the handler of the injection
        private val handlerThread = Thread.currentThread()

        @relayWorkerExecution
        def getPackets: Seq[Packet] = {
            val packets = injections
                .sorted((x: (Int, Packet), y: (Int, Packet)) => x._1 - y._1)
                .toArray
                .map(_._2)

            currentInjections.remove(injectableID)
            packets
        }

        @relayWorkerExecution
        def mayNotHandle: Boolean = {
            Thread.currentThread() != handlerThread
        }
    }


}

