package fr.overridescala.vps.ftp.api.utils

object Utils {

  def cut(src: Array[Byte], a: Array[Byte], b: Array[Byte]): Array[Byte] =
    java.util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.indexOfSlice(b))

  def cutString(src: Array[Byte], a: Array[Byte], b: Array[Byte]) =
    new String(cut(src, a, b))

}
