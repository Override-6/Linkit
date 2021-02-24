package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.Packet

sealed trait RefPacket[A <: Serializable] extends Packet {
    val value: A
    def casted[C <: A]: C = value.asInstanceOf[C]
}

object RefPacket {

    case class StringPacket(override val value: String) extends RefPacket[String]

    case class AnyRefPacket[A <: Serializable] private(override val value: A) extends RefPacket[A]

    case class ObjectPacket(override val value: Serializable) extends RefPacket[Serializable]

    //TODO Fix Array[Serializable] and Array[Any] cast exception
    case class ArrayRefPacket(override val value: Array[Any]) extends RefPacket[Array[Any]] {
        def apply(i: Int): Any = value(i)

        def isEmpty: Boolean = value.isEmpty

        def contains(a: Any): Boolean = value.contains(a)

        def length: Int = value.length

        override def toString: String = s"ArrayRefPacket(${value.mkString(",")})"
    }

    case class ArrayValPacket[A <: AnyVal](override val value: Array[A]) extends RefPacket[Array[A]] {
        def apply(i: Int): A = value(i)

        def isEmpty: Boolean = value.isEmpty

        def contains(a: A): Boolean = value.contains(a)

        def length: Int = value.length
    }

    def apply[A <: Serializable](value: A): AnyRefPacket[A] = AnyRefPacket(value)

    implicit def unbox[A <: Serializable](packet: RefPacket[A]): A = packet.value

}
