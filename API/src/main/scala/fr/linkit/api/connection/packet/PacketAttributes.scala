package fr.linkit.api.connection.packet

trait PacketAttributes extends Serializable {

    def getAttribute(name: String): Option[Serializable]

    def putAttribute(name: String, value: Serializable): Unit

}
