package fr.`override`.linkit.server.network

import fr.`override`.linkit.skull.connection.network.cache.SharedCacheHandler
import fr.`override`.linkit.skull.connection.packet.fundamental.RefPacket.ArrayObjectPacket
import fr.`override`.linkit.skull.connection.packet.fundamental.ValPacket.LongPacket
import fr.`override`.linkit.skull.connection.packet.traffic.PacketTraffic
import fr.`override`.linkit.skull.connection.packet.Packet

class ServerSharedCacheHandler(family: String, traffic: PacketTraffic) extends SharedCacheHandler(family, traffic) {
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

object ServerSharedCacheHandler {
    def apply(): (String, PacketTraffic) => ServerSharedCacheHandler = new ServerSharedCacheHandler(_, _)
}
