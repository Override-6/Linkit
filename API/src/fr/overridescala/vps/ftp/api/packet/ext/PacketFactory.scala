package fr.overridescala.vps.ftp.api.packet.ext

import java.util

import fr.overridescala.vps.ftp.api.packet.Packet

trait PacketFactory[T <: Packet] {

  def decompose(implicit packet: T): Array[Byte]

  def canTransform(implicit bytes: Array[Byte]): Boolean

  def build(implicit bytes: Array[Byte]): T


  // utility methods
  protected def cut(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
    util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.indexOfSlice(b))

  protected def cutEnd(a: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
    util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.length)

  protected def cutString(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]) =
    new String(cut(a, b))

}
