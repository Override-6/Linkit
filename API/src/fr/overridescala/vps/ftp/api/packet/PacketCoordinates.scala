package fr.overridescala.vps.ftp.api.packet

case class PacketCoordinates(channelID: Int, targetID: String, senderID: String) {

    override def toString: String = s"PacketCoordinates(channelId: $channelID, targetID: $targetID, senderID: $senderID)"

}