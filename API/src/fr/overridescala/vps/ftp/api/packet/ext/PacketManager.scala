package fr.overridescala.vps.ftp.api.packet.ext

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.Packet
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, EmptyPacket, ErrorPacket, TaskInitPacket}

import scala.collection.mutable


object PacketManager {
    val SizeSeparator: Array[Byte] = ":".getBytes
    val TargetSeparator: Array[Byte] = "<target>".getBytes
    val SenderSeparator: Array[Byte] = "<sender>".getBytes
}

class PacketManager {

    import PacketManager._

    type PT <: Packet

    private val factories = withFundamentals()

    def registerIfAbsent[P <: Packet](packetClass: Class[P], packetFactory: PacketFactory[P]): Unit = {
        val factory = packetFactory.asInstanceOf[PacketFactory[PT]]
        val ptClass = packetClass.asInstanceOf[Class[PT]]
        if (!factories.contains(ptClass))
            factories.put(ptClass, factory)
    }

    def toPacket(implicit bytes: Array[Byte]): Packet = {
        val sepIndex = bytes.indexOfSlice(SenderSeparator)
        val senderID = new String(bytes.slice(0, sepIndex))
        val targetID = new String(bytes.slice(sepIndex + SenderSeparator.length, bytes.indexOfSlice(TargetSeparator)))
        for (factory <- factories.values) {
            if (factory.canTransform(bytes))
                return factory.build(senderID, targetID)(bytes)
        }
        throw UnexpectedPacketException(s"could not find packet factory for ${new String(bytes)}")
    }

    def toBytes[D <: Packet](classOfP: Class[D], packet: D): Array[Byte] = {
        val packetBytes = factories(classOfP.asInstanceOf[Class[PT]])
                .asInstanceOf[PacketFactory[D]]
                .decompose(packet)
        val bytes = PacketUtils.redundantBytesOf(packet) ++ packetBytes
        bytes.length.toString.getBytes ++ SizeSeparator ++ bytes
    }

    def toBytes[D <: Packet](packet: D): Array[Byte] = {
        val packetClass = packet.getClass.asInstanceOf[Class[D]]
        toBytes(packetClass, packet)
    }

    private def withFundamentals(): mutable.Map[Class[PT], PacketFactory[PT]] = {
        val factories = mutable.LinkedHashMap.empty[Class[PT], PacketFactory[PT]]

        def put[D <: Packet](clazz: Class[D], fact: PacketFactory[D]): Unit =
            factories.put(clazz.asInstanceOf[Class[PT]], fact.asInstanceOf[PacketFactory[PT]])

        put(classOf[DataPacket], DataPacket.Factory)
        put(classOf[EmptyPacket], EmptyPacket.Factory)
        put(classOf[TaskInitPacket], TaskInitPacket.Factory)
        put(classOf[ErrorPacket], ErrorPacket.Factory)
        factories
    }


}
