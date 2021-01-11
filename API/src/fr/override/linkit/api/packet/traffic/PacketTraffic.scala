package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}


trait PacketTraffic extends PacketWriter with JustifiedCloseable {

    def register(injectable: PacketInjectable): Unit

    def unregister(id: Int, reason: CloseReason): Unit

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

    def isRegistered(identifier: Int): Boolean



}

object PacketTraffic {
    val SystemChannelID = 1
    val AsyncNetworkCollectorID = 2
    val SyncNetworkCollectorID = 3
    val RemoteConsolesCollectorID = 4
    val RemoteFragmentsReqCollectorID = 5
    val RemoteFragmentsRespCollectorID = 6
}
