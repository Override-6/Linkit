package fr.linkit.core.connection.packet

import fr.linkit.api.connection.packet.{PacketAttributes, PacketAttributesPresence}

abstract class AbstractAttributePresence extends PacketAttributesPresence {

    protected val defaultAttributes: PacketAttributes = SimplePacketAttributes.empty

    override def getID: Int

    override def addDefaultAttribute(key: Serializable, value: Serializable): this.type = {
        defaultAttributes.putAttribute(key, value)
        this
    }

    override def addDefaultPresence(presence: PacketAttributesPresence, value: Serializable): this.type = {
        defaultAttributes.putPresence(presence, value)
        this
    }

    override def getDefaultProperty[S](key: Serializable): Option[S] = {
        defaultAttributes.getAttribute(key)
    }

    override def getDefaultPresence[S](presence: PacketAttributesPresence): Option[S] = {
        defaultAttributes.getPresence(presence)
    }

    protected def drainAllAttributes(attributes: PacketAttributes): this.type = {
        defaultAttributes.drainAttributes(attributes)
        this
    }

}

