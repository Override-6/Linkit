package fr.`override`.linkit.api.connection.packet

//TODO Doc
trait Packet extends Serializable {

    def className: String = getClass.getSimpleName

}