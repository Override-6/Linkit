package fr.`override`.linkit.api.connection.packet

import fr.`override`.linkit.api.connection.packet.serialization.Serializer

case class DedicatedPacketCoordinates(injectableID: Int, targetID: String, senderID: String) extends PacketCoordinates {

    override def toString: String = s"DedicatedPacketCoordinates(channelId: $injectableID, targetID: $targetID, senderID: $senderID)"

    def reversed(): DedicatedPacketCoordinates = DedicatedPacketCoordinates(injectableID, senderID, targetID)

    override def determineSerializer(cachedWhitelist: Array[String], raw: Serializer, cached: Serializer): Serializer = {
        if (cachedWhitelist.contains(targetID)) cached else raw
    }
}
