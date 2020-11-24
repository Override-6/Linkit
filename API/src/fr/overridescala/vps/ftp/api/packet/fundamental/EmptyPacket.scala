package fr.overridescala.vps.ftp.api.packet.fundamental

import fr.overridescala.vps.ftp.api.`extension`.packet.PacketFactory
import fr.overridescala.vps.ftp.api.packet.Packet

case class EmptyPacket private() extends Packet {

}

object EmptyPacket extends PacketFactory[EmptyPacket] {

    private val Empty = new EmptyPacket

    def apply(): EmptyPacket = Empty

    override def decompose(implicit packet: EmptyPacket): Array[Byte] =
        new Array[Byte](0)

    override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.isEmpty

    override def build(implicit bytes: Array[Byte]): EmptyPacket =
        Empty

    override val packetClass: Class[EmptyPacket] = classOf[EmptyPacket]
}

