package fr.`override`.linkit.api.packet

//TODO Doc
trait Packet extends Serializable {

    def className: String = getClass.getSimpleName

}