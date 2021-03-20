package fr.`override`.linkit.api.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.JustifiedCloseable


trait PacketTraffic extends JustifiedCloseable with PacketInjectableContainer  {

    val relayID: String
    val ownerID: String

    @workerExecution
    def handleInjection(injection: PacketInjection): Unit

    def canConflict(id: Int, scope: ChannelScope): Boolean

    def newWriter(id: Int, transform: Packet => Packet = p => p): PacketWriter

    def translator: PacketTranslator

}

object PacketTraffic {
    val SystemChannelID = 1
    val RemoteConsoles = 2
}