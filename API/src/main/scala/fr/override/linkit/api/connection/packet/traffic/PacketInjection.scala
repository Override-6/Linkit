package fr.`override`.linkit.api.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.local.concurrency.workerExecution

trait PacketInjection {

    val coordinates: DedicatedPacketCoordinates

    @workerExecution
    def getPackets: Seq[Packet]

    @workerExecution
    def mayNotHandle: Boolean

}
