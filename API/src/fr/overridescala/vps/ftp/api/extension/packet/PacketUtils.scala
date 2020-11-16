package fr.overridescala.vps.ftp.api.`extension`.packet

import java.util

import fr.overridescala.vps.ftp.api.packet.Packet

object PacketUtils {

    def cut(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
        util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.indexOfSlice(b))

    def cutEnd(a: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
        util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.length)

    def cutEndString(a: Array[Byte])(implicit src: Array[Byte]): String =
        new String(cutEnd(a))

    def cutString(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]) =
        new String(cut(a, b))

    def redundantBytesOf(packet: Packet): Array[Byte] = {
        val channelID = packet.channelID.toString.getBytes
        val targetID = packet.targetID.getBytes
        val senderID = packet.senderID.getBytes
        channelID ++ PacketManager.ChannelIDSeparator ++
                senderID ++ PacketManager.SenderSeparator ++
                targetID ++ PacketManager.TargetSeparator
    }

}