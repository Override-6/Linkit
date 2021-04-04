package fr.linkit.core.connection.packet.traffic.channel.request

import fr.linkit.api.connection.packet.Packet
import fr.linkit.core.local.utils.ScalaUtils.ensureType

import java.util.NoSuchElementException
import scala.reflect.ClassTag

sealed abstract class SubmitterPacket(packets: Array[Packet], private val properties: Map[String, Serializable]) extends Packet {

    @transient private var packetIndex = 0

    def getProperty[P <: Serializable](name: String): Option[P] = {
        properties.get(name) match {
            case value: Option[P] => value
            case _                => None
        }
    }

    @throws[NoSuchElementException]("If this method is called more times than packet array's length" + this)
    def nextPacket[P <: Packet : ClassTag]: P = {
        if (packetIndex >= packets.length)
            throw new NoSuchElementException()

        val packet = packets(packetIndex)
        packetIndex += 1
        ensureType[P](packet)
    }

    def foreach(action: Packet => Unit): Unit = {
        packets.foreach(action)
    }

}

case class ResponsePacket(id: Long, packets: Array[Packet], private val properties: Map[String, Serializable])
        extends SubmitterPacket(packets, properties)

case class RequestPacket(id: Long, packets: Array[Packet], private val properties: Map[String, Serializable])
        extends SubmitterPacket(packets, properties)
