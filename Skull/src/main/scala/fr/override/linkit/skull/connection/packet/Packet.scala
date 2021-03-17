package fr.`override`.linkit.skull.connection.packet

//TODO Doc
trait Packet extends Serializable {

    def className: String = getClass.getSimpleName

}