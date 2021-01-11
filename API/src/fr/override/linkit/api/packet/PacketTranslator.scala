package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.{PacketException, UnexpectedPacketException}
import fr.`override`.linkit.api.packet.PacketUtils.wrap
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.system.SystemPacket
import fr.`override`.linkit.api.utils.cache.ObjectPacket
import fr.`override`.linkit.api.utils.{Tuple2Packet, Tuple3Packet, WrappedPacket}

import scala.collection.mutable


object PacketTranslator {
    val ChannelIDSeparator: Array[Byte] = "<channel>".getBytes
    val SenderSeparator: Array[Byte] = "<sender>".getBytes
    val TargetSeparator: Array[Byte] = "<target>".getBytes
}

class PacketTranslator(relay: Relay) { //Notifier is accessible from api to reduce parameter number in (A)SyncPacketChannel

    private val factories = mutable.LinkedHashMap.empty[Class[_ <: Packet], PacketFactory[_ <: Packet]]
    registerDefaults()

    def toPacketAndCoords(bytes: Array[Byte]): (Packet, PacketCoordinates) = {
        val (coordinates, coordsLength) = PacketUtils.getCoordinates(bytes)

        val customPacketBytes = bytes.slice(coordsLength, bytes.length)
        (toPacket(customPacketBytes), coordinates)
    }

    def toPacket(bytes: Array[Byte]): Packet = {
        if (bytes.length > relay.configuration.maxPacketLength)
            throw PacketException("Custom packet bytes length exceeded configuration limit")

        for (factory <- factories.values) {
            if (factory.canTransform(this)(bytes))
                return factory.build(this)(bytes)
        }
        throw new UnexpectedPacketException(s"Could not find packet factory for ${new String(bytes)}")
    }

    def fromPacketAndCoords(packet: Packet, coordinates: PacketCoordinates): Array[Byte] = {
        wrap(fromPacketAndCoordsNoWrap(packet, coordinates))
    }

    def fromPacketAndCoordsNoWrap(packet: Packet, coordinates: PacketCoordinates): Array[Byte] = {
        val packetBytes = fromPacket(packet)
        val bytes = PacketUtils.getCoordinatesBytes(coordinates) ++ packetBytes
        relay.securityManager.hashBytes(bytes)
    }

    def fromPacket(packet: Packet): Array[Byte] = {
        val packetClass = packet.getClass
        factories(packetClass)
                .asInstanceOf[PacketFactory[Packet]]
                .decompose(this)(packet)
    }

    private def registerDefaults(): Unit = {
        Array(
            DataPacket, EmptyPacket.Factory,
            TaskInitPacket, ErrorPacket,
            SystemPacket, WrappedPacket, ObjectPacket,
            Tuple2Packet, Tuple3Packet
        ).foreach(registerFactory)
    }

    def registerFactory(packetFactory: PacketFactory[_ <: Packet]): Unit = {
        val factory = packetFactory
        val ptClass = packetFactory.packetClass

        if (factories.contains(ptClass))
            throw new IllegalArgumentException(s"Packet '$ptClass' type is already registered !")

        factories.put(ptClass, factory)
        //notifier.onPacketTypeRegistered(ptClass, packetFactory) //TODO
    }


}
