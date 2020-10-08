package fr.overridescala.vps.ftp.api.packet.ext

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.Packet

import scala.collection.mutable

class PacketManager {

  type P = _ <: Packet
  private val factories = mutable.Map.empty[Class[P], PacketFactory[P]]

  def registerPacketFactory(classOfP: Class[P], packetFactory: PacketFactory[P]): Unit =
    factories.put(classOfP, packetFactory)

  def toPacket(bytes: Array[Byte]): Packet = {
    for (factory <- factories.values) {
      if (factory.canTransform(bytes))
        return factory.toPacket(bytes)
    }
    throw UnexpectedPacketException(s"could not find packet factory for ${new String(bytes)}")
  }

  def toBytes(classOfP: Class[P], packet: P): Array[Byte] = {
    factories(classOfP).toBytes(packet)
  }

  implicit def autoBytes(packet: P): Array[Byte] = {
    try {
      val packetClass = packet.getClass.asInstanceOf[Class[P]]
      toBytes(packetClass, packet)
    }
  }

}