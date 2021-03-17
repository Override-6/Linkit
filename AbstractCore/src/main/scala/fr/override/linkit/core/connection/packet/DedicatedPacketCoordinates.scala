package fr.`override`.linkit.core.connection.packet

import fr.`override`.linkit.skull.connection.packet.serialization.ObjectSerializer

case class DedicatedPacketCoordinates(injectableID: Int, targetID: String, senderID: String) extends PacketCoordinates {

    override def toString: String = s"DedicatedPacketCoordinates(channelId: $injectableID, targetID: $targetID, senderID: $senderID)"

    def reversed(): DedicatedPacketCoordinates = DedicatedPacketCoordinates(injectableID, senderID, targetID)

    override def determineSerializer(cachedWhitelist: Array[String], raw: ObjectSerializer, cached: ObjectSerializer): ObjectSerializer = {
        if (cachedWhitelist.contains(targetID)) cached else raw
    }
}
