package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketTranslator, PacketUtils}
import fr.`override`.linkit.api.utils.Utils

case class ObjectPacket(obj: Serializable) extends Packet {
    //Cache the serialised object in order to avoid the factory
    //To serialise the object every time we want to send the same packet
    private val objBytes = Utils.serialize(obj)

    def casted[T <: Serializable]: T = obj.asInstanceOf[T]
}

object ObjectPacket extends PacketFactory[ObjectPacket] {

    private lazy val Type = "[obj]".getBytes
    override val packetClass: Class[ObjectPacket] = classOf[ObjectPacket]

    override def decompose(translator: PacketTranslator)(implicit packet: ObjectPacket): Array[Byte] = {
        Type ++ packet.objBytes
    }

    override def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean = {
        bytes.startsWith(Type)
    }

    override def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): ObjectPacket = {
        val objectBytes = PacketUtils.untilEnd(Type)
        ObjectPacket(Utils.deserialize(objectBytes))
    }

    implicit def asPacket(obj: Serializable): ObjectPacket = ObjectPacket(obj)

}
