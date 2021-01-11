package fr.`override`.linkit.api.utils

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketTranslator, PacketUtils}

class Tuple2Packet(val _1: String, val _2: String) extends Packet

object Tuple2Packet extends PacketFactory[Tuple2Packet] {

    override val packetClass: Class[Tuple2Packet] = classOf[Tuple2Packet]
    private val Type = "[tpl3]".getBytes
    private val Two = "<2>".getBytes

    override def decompose(translator: PacketTranslator)(implicit packet: Tuple2Packet): Array[Byte] = {
        val one = packet._1.getBytes
        val two = packet._2.getBytes
        Type ++ one ++ Two ++ two
    }

    override def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean = bytes.startsWith(Type)

    override def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): Tuple2Packet = {
        val _1 = PacketUtils.stringBetween(Type, Two)
        val _2 = PacketUtils.stringUntilEnd(Two)

        Tuple2Packet(_1, _2)
    }

    implicit def from(_1: String, _2: String): Tuple2Packet = new Tuple2Packet(_1, _2)
    implicit def apply(_1: String, _2: String): Tuple2Packet = from(_1, _2)

}