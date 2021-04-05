package fr.linkit.core.connection.packet

import fr.linkit.api.connection.packet.PacketAttributes

object EmptyPacketAttributes extends PacketAttributes {

    override def getAttribute(name: String): Option[Serializable] = None

    override def putAttribute(name: String, value: Serializable): Unit = ()

    override def toString: String = "EmptyPacketAttributes"

}
