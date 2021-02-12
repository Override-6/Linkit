package fr.`override`.linkit.api.packet

import java.util

import fr.`override`.linkit.api.utils.ScalaUtils

object PacketUtils {

    def stringUntilEnd(a: Array[Byte])(implicit src: Array[Byte]): String =
        new String(untilEnd(a))

    def untilEnd(a: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
        util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.length)

    def stringBetween(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]) =
        new String(between(a, b))

    def between(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
        util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.indexOfSlice(b))

    def wrap(bytes: Array[Byte]): Array[Byte] = {
        ScalaUtils.fromInt(bytes.length) ++ bytes
    }

}
