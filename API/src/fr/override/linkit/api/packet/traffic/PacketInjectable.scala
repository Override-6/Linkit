package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.JustifiedCloseable

trait PacketInjectable extends JustifiedCloseable {
    val identifier: Int
    val ownerID: String
    /**
     * The traffic handler that is directly or not injecting the packets
     * */
    val traffic: PacketTraffic

    @relayWorkerExecution
    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

}
