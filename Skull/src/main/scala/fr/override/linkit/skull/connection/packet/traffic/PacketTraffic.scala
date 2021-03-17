package fr.`override`.linkit.skull.connection.packet.traffic

import fr.`override`.linkit.internal.concurrency.relayWorkerExecution
import fr.`override`.linkit.skull.connection.packet.Packet
import .PacketInjection
import fr.`override`.linkit.skull.internal.system.JustifiedCloseable


trait PacketTraffic extends JustifiedCloseable with PacketInjectableContainer  {

    val relayID: String
    val ownerID: String

    @relayWorkerExecution
    def handleInjection(injection: PacketInjection): Unit

    def canConflict(id: Int, scope: ChannelScope): Boolean

    def newWriter(id: Int, transform: Packet => Packet = p => p): PacketWriter

}

object PacketTraffic {
    val SystemChannelID = 1
    val RemoteConsoles = 2
}