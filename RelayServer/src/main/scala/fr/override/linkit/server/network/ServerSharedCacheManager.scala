package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.core.connection.network.cache.AbstractSharedCacheManager
import fr.`override`.linkit.core.connection.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.`override`.linkit.core.connection.packet.fundamental.ValPacket.LongPacket

class ServerSharedCacheManager(family: String, owner: String, traffic: PacketTraffic) extends AbstractSharedCacheManager(family, owner, traffic) {
    override def continuePacketHandling(packet: Packet, coords: DedicatedPacketCoordinates): Unit = packet match {
        case LongPacket(cacheID) =>
            val senderID: String = coords.senderID
            println(s"RECEIVED CONTENT REQUEST FOR IDENTIFIER $cacheID REQUESTOR : $senderID")
            val content = LocalCacheHandler.getContentOrElseMock(cacheID)
            println(s"Content = ${content.mkString("Array(", ", ", ")")}")
            communicator.sendResponse(ArrayObjectPacket(content), senderID)
            println("Packet sent :D")
    }

}

object ServerSharedCacheManager {
    def apply(): (String, String, PacketTraffic) => ServerSharedCacheManager = new ServerSharedCacheManager(_, _, _)
}
