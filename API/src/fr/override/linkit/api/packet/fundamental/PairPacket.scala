package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.Packet

case class PairPacket(tag: String, value: Any) extends Packet {
    def casted[A]: A = value.asInstanceOf[A]
}

