package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.Packet

case class ValPacket(value: Serializable) extends Packet {
    def casted[T <: Serializable]: T = value.asInstanceOf[T]
}
