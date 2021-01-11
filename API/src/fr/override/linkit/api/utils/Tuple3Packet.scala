package fr.`override`.linkit.api.utils

import fr.`override`.linkit.api.packet.{PacketFactory, PacketTranslator, PacketUtils}

class Tuple3Packet(_1: String, _2: String, val _3: String) extends Tuple2Packet(_1, _2)

object Tuple3Packet extends PacketFactory[Tuple3Packet] {

    private val Type = "[tpl3]".getBytes
    private val Two = "<2>".getBytes
    private val Three = "<3>".getBytes

    override def decompose(translator: PacketTranslator)(implicit packet: Tuple3Packet): Array[Byte] = {
        val one = packet._1.getBytes
        val two = packet._2.getBytes
        val three = packet._3.getBytes
        Type ++ one ++ Two ++ two ++ Three ++ three
    }

    override def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean = bytes.startsWith(Type)

    override def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): Tuple3Packet = {
        val _1 = PacketUtils.stringBetween(Type, Two)
        val _2 = PacketUtils.stringBetween(Two, Three)
        val _3 = PacketUtils.stringUntilEnd(Three)

        Tuple3Packet(_1, _2, _3)
    }

    override val packetClass: Class[Tuple3Packet] = classOf[Tuple3Packet]


    implicit def from(_1: String, _2: String, _3: String): Tuple3Packet = new Tuple3Packet(_1, _2, _3)

    implicit def apply(_1: String, _2: String, _3: String): Tuple3Packet = from(_1, _2, _3)
}