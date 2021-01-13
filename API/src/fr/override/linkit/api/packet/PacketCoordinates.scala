package fr.`override`.linkit.api.packet

//TODO optimised PacketCoordinates by caching every possibility
case class PacketCoordinates(injectableID: Int, targetID: String, senderID: String) {

    override def toString: String = s"PacketCoordinates(channelId: $injectableID, targetID: $targetID, senderID: $senderID)"

    def reversed(): PacketCoordinates = PacketCoordinates(injectableID, senderID, targetID)

}