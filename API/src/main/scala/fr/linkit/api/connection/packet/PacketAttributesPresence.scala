package fr.linkit.api.connection.packet

//TODO -------------------------------------------------- MAINTAINED --------------------------------------------------

trait PacketAttributesPresence {

    def addDefaultAttribute(key: Serializable, value: Serializable): this.type

    //def addDefaultPresence(presence: PacketAttributesPresence, value: Serializable): this.type

    def getDefaultAttribute[S](key: Serializable): Option[S]

    //def getDefaultPresence[S](presence: PacketAttributesPresence): Option[S]

    def drainAllAttributes(packetAttributes: PacketAttributes): this.type

    def drainAllDefaultAttributes(packetAttributes: PacketAttributesPresence): this.type

}
