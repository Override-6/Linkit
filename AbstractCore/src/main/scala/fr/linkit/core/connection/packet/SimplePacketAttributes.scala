package fr.linkit.core.connection.packet

import fr.linkit.api.connection.packet.PacketAttributes
import fr.linkit.api.local.system.AppLogger

import scala.collection.mutable

class SimplePacketAttributes extends PacketAttributes {

    protected[packet] val attributes: mutable.Map[Serializable, Serializable] = mutable.HashMap[Serializable, Serializable]()

    def this(attributes: SimplePacketAttributes) {
        this()
        attributes.attributes.foreachEntry(putAttribute)
    }

    override def getAttribute[S](name: Serializable): Option[S] = attributes.get(name) match {
        case o: Option[S] => o
        case _            => None
    }

    override def putAttribute(name: Serializable, value: Serializable): this.type = {
        attributes.put(name, value)
        AppLogger.vError(s"Attribute put ($name -> $value), $attributes - $hashCode")
        this
    }

    override def equals(obj: Any): Boolean = obj match {
        case s: SimplePacketAttributes => s.attributes == attributes
        case _                         => false
    }

    override def hashCode(): Int = attributes.hashCode()

    override def toString: String = attributes.mkString("SimplePacketAttributes(", ", ", ")")

    override def drainAttributes(other: PacketAttributes): this.type = {
        foreachAttributes((k, v) => other.putAttribute(k, v))
    }

    override def foreachAttributes(f: (Serializable, Serializable) => Unit): this.type = {
        attributes.foreachEntry(f)
        this
    }

    override def isEmpty: Boolean = attributes.isEmpty
}

object SimplePacketAttributes {

    def empty: SimplePacketAttributes = new SimplePacketAttributes

    def apply(): SimplePacketAttributes = empty

    def apply(attributes: SimplePacketAttributes): SimplePacketAttributes = new SimplePacketAttributes(attributes)

    def from(tuples: (String, Serializable)*): SimplePacketAttributes = {
        val atr = empty
        tuples.foreach(tuple => atr.putAttribute(tuple._1, tuple._2))
        atr
    }
}
