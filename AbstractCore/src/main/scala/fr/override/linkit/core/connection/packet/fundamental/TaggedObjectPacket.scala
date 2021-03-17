package fr.`override`.linkit.core.connection.packet.fundamental

import fr.`override`.linkit.skull.connection.packet.Packet

/**
 * Represents a packet of a tag string and a serializable value.
 * */
case class TaggedObjectPacket(tag: String, value: Serializable) extends Packet {
    def casted[A <: Serializable]: A = value.asInstanceOf[A]
}

