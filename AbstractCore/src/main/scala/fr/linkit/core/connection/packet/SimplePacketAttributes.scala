package fr.linkit.core.connection.packet

import fr.linkit.api.connection.packet.PacketAttributes

import scala.collection.mutable

class SimplePacketAttributes extends PacketAttributes {

    private val attributes = mutable.HashMap[String, Serializable]()

    override def getAttribute(name: String): Option[Serializable] = attributes.get(name)

    override def putAttribute(name: String, value: Serializable): Unit = attributes.put(name, value)

    override def equals(obj: Any): Boolean = obj match {
        case s: SimplePacketAttributes => s.attributes == attributes
        case _                         => false
    }

    override def hashCode(): Int = attributes.hashCode()

    override def toString: String = attributes.mkString("SimplePacketAttributes(", ", ", ")")

}

object SimplePacketAttributes {

    def empty: SimplePacketAttributes = new SimplePacketAttributes

    def from(tuples: (String, Serializable)*): SimplePacketAttributes = {
        val atr = empty
        tuples.foreach(tuple => atr.putAttribute(tuple._1, tuple._2))
        atr
    }
}
