package fr.`override`.linkit.api.utils

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketUtils}

case class Tuple3Packet(_1: String, _2: String, _3: String) extends Packet

object Tuple3Packet extends PacketFactory[Tuple3Packet] {

    private val Type = "[tpl3]".getBytes
    private val Two = "<2>".getBytes
    private val Three = "<3>".getBytes

    override def decompose(implicit packet: Tuple3Packet): Array[Byte] = {
        val one = packet._1.getBytes
        val two = packet._2.getBytes
        val three = packet._3.getBytes
        Type ++ one ++ Two ++ two ++ Three ++ three
    }

    override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.startsWith(Type)

    override def build(implicit bytes: Array[Byte]): Tuple3Packet = {
        val _1 = PacketUtils.cutString(Type, Two)
        val _2 = PacketUtils.cutString(Two, Three)
        val _3 = PacketUtils.cutEndString(Three)

        Tuple3Packet(_1, _2, _3)
    }

    override val packetClass: Class[Tuple3Packet] = classOf[Tuple3Packet]

    implicit def toPacket(tuple3: (String, String, String)): Tuple3Packet =
        Tuple3Packet(tuple3._1, tuple3._2, tuple3._3)

    implicit def toPacket(tuple2: (String, String)): Tuple3Packet =
        Tuple3Packet(tuple2._1, tuple2._2, "")
}