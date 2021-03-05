package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.Packet

/**
 * Represents a packet of a tag string and a serializable value.
 * */
case class TaggedObjectPacket(tag: String, value: Serializable) extends Packet {
    def casted[A <: Serializable]: A = value.asInstanceOf[A]
}

