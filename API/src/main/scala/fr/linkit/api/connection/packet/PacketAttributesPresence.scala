package fr.linkit.api.connection.packet

trait PacketAttributesPresence {

    def getID: Int

    def addDefaultAttribute(key: Serializable, value: Serializable): this.type

    def addDefaultPresence(presence: PacketAttributesPresence, value: Serializable): this.type

    def getDefaultProperty[S](key: Serializable): Option[S]

    def getDefaultPresence[S](presence: PacketAttributesPresence): Option[S]

}
