package fr.overridescala.vps.ftp.`extension`.cloud.sync

import java.nio.file.Path

import fr.overridescala.vps.ftp.api.`extension`.packet.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel, PacketUtils}


case class FolderSyncPacket(override val channelID: Int,
                            override val targetID: String,
                            override val senderID: String,
                            order: String,
                            affectedPath: String,
                            content: Array[Byte]) extends Packet

object FolderSyncPacket extends PacketFactory[FolderSyncPacket] {

    def apply(order: String, affectedPath: Path, content: Array[Byte] = Array())(implicit channel: PacketChannel): FolderSyncPacket = {
        new FolderSyncPacket(
            channel.channelID, channel.connectedID,
            channel.ownerID, order,
            affectedPath.toString, content
        )
    }

    private val Type = "[fsync]".getBytes()
    private val Affected = "<affected>".getBytes()
    private val Content = "<content>".getBytes()

    override def decompose(implicit packet: FolderSyncPacket): Array[Byte] = {
        val orderBytes = packet.order.getBytes()
        val affectedBytes = packet.affectedPath.getBytes()
        Type ++ orderBytes ++ Affected ++ affectedBytes ++ Content ++ packet.content
    }

    override def canTransform(implicit bytes: Array[Byte]): Boolean = {
        bytes.startsWith(Type)
    }

    override def build(channelID: Int, senderID: String, targetID: String)(implicit bytes: Array[Byte]): FolderSyncPacket = {
        val order = PacketUtils.cutString(Type, Affected)
        val affectedPath = PacketUtils.cutString(Affected, Content)
        val content = PacketUtils.cutEnd(Content)

        new FolderSyncPacket(channelID, senderID, targetID, order, affectedPath, content)
    }
}
