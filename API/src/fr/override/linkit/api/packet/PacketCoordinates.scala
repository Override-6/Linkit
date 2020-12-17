package fr.`override`.linkit.api.packet

case class PacketCoordinates(containerID: Int, targetID: String, senderID: String) {

    override def toString: String = s"PacketCoordinates(channelId: $containerID, targetID: $targetID, senderID: $senderID)"

}