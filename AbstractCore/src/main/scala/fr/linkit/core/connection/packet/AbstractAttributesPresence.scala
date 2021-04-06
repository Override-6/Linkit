package fr.linkit.core.connection.packet

import fr.linkit.api.connection.packet.{PacketAttributes, PacketAttributesPresence}

abstract class AbstractAttributesPresence extends PacketAttributesPresence {

    protected var defaultAttributes: PacketAttributes = SimplePacketAttributes.empty

    override def addDefaultAttribute(key: Serializable, value: Serializable): this.type = {
        defaultAttributes.putAttribute(key, value)
        this
    }

    override def getDefaultProperty[S](key: Serializable): Option[S] = {
        defaultAttributes.getAttribute(key)
    }

    protected def drainAllAttributes(attributes: PacketAttributes): this.type = {
        defaultAttributes.drainAttributes(attributes)
        this
    }

}

