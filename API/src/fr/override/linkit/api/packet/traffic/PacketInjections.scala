package fr.`override`.linkit.api.packet.traffic

import java.util.concurrent.ConcurrentHashMap

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.packet.{DedicatedPacketCoordinates, Packet}

import scala.collection.mutable.ListBuffer

object PacketInjections {
    private val currentInjections = new ConcurrentHashMap[(Int, String), PacketInjection]

    def createInjection(packet: Packet, coordinates: DedicatedPacketCoordinates, number: Int): PacketInjection = this.synchronized {
        //println(s"CREATING INJECTION FOR PACKET $packet WITH COORDINATES $coordinates, $number")
        val id = coordinates.injectableID
        val sender = coordinates.senderID

        var injection = currentInjections.get((id, sender))
        if (injection == null) {
            injection = new PacketInjection(coordinates)
            currentInjections.put((id, sender), injection)
        }

        injection.injections += ((number, packet))
        injection
    }

    def unhandled(coordinates: DedicatedPacketCoordinates, packets: Packet*): PacketInjection = {
        //println(s"NEW UNHANDLED FOR COORDINATES $coordinates / PACKETS ${packets}")
        val injection = new PacketInjection(coordinates)
        var i = 0
        packets.foreach(packet => {
            i += 1
            injection.injections += ((i, packet))
        })
        injection
    }

    class PacketInjection private[PacketInjections](val coordinates: DedicatedPacketCoordinates) {

        private[PacketInjections] val injections = ListBuffer.empty[(Int, Packet)]
        private val injectableID = coordinates.injectableID
        //The thread that created this object
        //will be set as the handler of the injection
        private[traffic] val handlerThread = Thread.currentThread()

        @relayWorkerExecution
        def getPackets: Seq[Packet] = {
            val packets = injections
                    .sorted((x: (Int, Packet), y: (Int, Packet)) => x._1 - y._1)
                    .toArray
                    .map(_._2)
            //println(s"DISCOVERED PACKETS ${packets.mkString("Array(", ", ", ")")} vs INJECTIONS $injections")
            currentInjections.remove((injectableID, coordinates.senderID))
            packets
        }

        @relayWorkerExecution
        def mayNotHandle: Boolean = {
            Thread.currentThread() != handlerThread
        }
    }


}

