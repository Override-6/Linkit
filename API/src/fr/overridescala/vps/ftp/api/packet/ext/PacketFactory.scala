package fr.overridescala.vps.ftp.api.packet.ext

import java.util

import fr.overridescala.vps.ftp.api.packet.Packet

trait PacketFactory[T <: Packet] {

  def toBytes(implicit packet: T): Array[Byte]

  def canTransform(implicit bytes: Array[Byte]): Boolean

  def toPacket(implicit bytes: Array[Byte]): T

  protected def cut(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
    util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.indexOfSlice(b))

  protected def cutEnd(a: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
    util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.length)

  protected def cutString(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]) =
    new String(cut(a, b))

}
