package fr.overridescala.vps.ftp.api.packet.ext

import java.util

import fr.overridescala.vps.ftp.api.packet.Packet

trait PacketFactory[T <: Packet] {

  def toBytes(packet: T): Array[Byte]

  def canTransform(bytes: Array[Byte]): Boolean

  def toPacket(bytes: Array[Byte]): T

  protected def cut(src: Array[Byte], a: Array[Byte], b: Array[Byte]): Array[Byte] =
    util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.indexOfSlice(b))

  protected def cutEnd(src: Array[Byte], a: Array[Byte]): Array[Byte] =
    util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.length)

  protected def cutString(src: Array[Byte], a: Array[Byte], b: Array[Byte]) =
    new String(cut(src, a, b))

}
