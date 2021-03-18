package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.traffic.PacketInjection
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.local.concurrency.workerExecution

import scala.collection.mutable.ListBuffer

class DirectInjection(override val coordinates: DedicatedPacketCoordinates) extends PacketInjection {

    private[PacketInjections] val injections = ListBuffer.empty[(Int, Packet)]
    private val injectableID = coordinates.injectableID
    //The thread that created this object
    //will be set as the handler of the injection
    private[traffic] val handlerThread = Thread.currentThread()

    @workerExecution
    override def getPackets: Seq[Packet] = {
        val packets = injections
            .sorted((x: (Int, Packet), y: (Int, Packet)) => x._1 - y._1)
            .toArray
            .map(_._2)
        //println(s"DISCOVERED PACKETS ${packets.mkString("Array(", ", ", ")")} vs INJECTIONS $injections")
        PacketInjections.currentInjections.remove((injectableID, coordinates.senderID))
        packets
    }

    @workerExecution
    override def mayNotHandle: Boolean = {
        Thread.currentThread() != handlerThread
    }

    def addPacket(packetNumber: Int, packet: Packet): Unit = {
        injections += ((packetNumber, packet))
    }
}
