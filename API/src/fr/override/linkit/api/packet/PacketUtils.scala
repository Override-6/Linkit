package fr.`override`.linkit.api.packet

import java.util

import fr.`override`.linkit.api.packet.PacketTranslator.{ChannelIDSeparator, SenderSeparator, TargetSeparator}

object PacketUtils {

    def cut(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
        util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.indexOfSlice(b))

    def cutEnd(a: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
        util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.length)

    def cutEndString(a: Array[Byte])(implicit src: Array[Byte]): String =
        new String(cutEnd(a))

    def cutString(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]) =
        new String(cut(a, b))


    def getCoordinatesBytes(coords: PacketCoordinates): Array[Byte] = {
        val identifier = coords.injectableID.toString.getBytes
        val targetID = coords.targetID.getBytes
        val senderID = coords.senderID.getBytes
        identifier ++ PacketTranslator.ChannelIDSeparator ++
                senderID ++ PacketTranslator.SenderSeparator ++
                targetID ++ PacketTranslator.TargetSeparator
    }

    def getCoordinates(bytes: Array[Byte]): (PacketCoordinates, Int) = {
        val channelIndex = bytes.indexOfSlice(ChannelIDSeparator)
        val senderIndex = bytes.indexOfSlice(SenderSeparator)
        val targetIndex = bytes.indexOfSlice(TargetSeparator)

        val containerID = new String(bytes.slice(0, channelIndex)).toInt
        val senderID = new String(bytes.slice(channelIndex + ChannelIDSeparator.length, senderIndex))
        val targetID = new String(bytes.slice(senderIndex + SenderSeparator.length, targetIndex))

        (PacketCoordinates(containerID, targetID, senderID), targetIndex + TargetSeparator.length)
    }

    def wrap(bytes: Array[Byte]): Array[Byte] = {
        val lengthBytes = bytes.length.toString.getBytes
        val flagLength = lengthBytes.length

        if (flagLength > 16)
            throw new UnsupportedOperationException("flagLength > 16")

        val packetLengthBytesLength = Integer.toHexString(flagLength).getBytes
        val result = packetLengthBytesLength ++ lengthBytes ++ bytes
        result
    }

}
