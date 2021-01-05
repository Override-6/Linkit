package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.{PacketException, UnexpectedPacketException}
import fr.`override`.linkit.api.packet.PacketUtils.wrap
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.system.SystemPacket
import fr.`override`.linkit.api.utils.Tuple3Packet

import scala.collection.mutable


object PacketTranslator {
    val ChannelIDSeparator: Array[Byte] = "<channel>".getBytes
    val SenderSeparator: Array[Byte] = "<sender>".getBytes
    val TargetSeparator: Array[Byte] = "<target>".getBytes
}

class PacketTranslator(relay: Relay) { //Notifier is accessible from api to reduce parameter number in (A)SyncPacketChannel

    private val factories = mutable.LinkedHashMap.empty[Class[_ <: Packet], PacketFactory[_ <: Packet]]
    registerDefaults()

    private def registerDefaults(): Unit = {
        registerFactory(DataPacket)
        registerFactory(EmptyPacket.Factory)
        registerFactory(TaskInitPacket)
        registerFactory(ErrorPacket)
        registerFactory(SystemPacket)
        registerFactory(Tuple3Packet)
    }

    def toPacket(implicit bytes: Array[Byte]): (Packet, PacketCoordinates) = {
        val (coordinates, coordsLength) = PacketUtils.getCoordinates(bytes)

        val customPacketBytes = bytes.slice(coordsLength, bytes.length)
        if (customPacketBytes.length > relay.configuration.maxPacketLength)
            throw PacketException("Custom packet bytes length exceeded configuration limit")

        for (factory <- factories.values) {
            if (factory.canTransform(customPacketBytes))
                return (factory.build(customPacketBytes), coordinates)
        }
        throw new UnexpectedPacketException(s"Could not find packet factory for ${new String(bytes)}")
    }

    def toBytes[P <: Packet](classOfP: Class[P], packet: P, coordinates: PacketCoordinates): Array[Byte] = {
        val packetBytes = factories(classOfP)
            .asInstanceOf[PacketFactory[P]]
            .decompose(packet)
        val bytes = PacketUtils.getCoordinatesBytes(coordinates) ++ packetBytes
        wrap(relay.securityManager.hashBytes(bytes))
    }

    def toBytes[D <: Packet](packet: D, coordinates: PacketCoordinates): Array[Byte] = {
        val packetClass = packet.getClass.asInstanceOf[Class[D]]
        toBytes(packetClass, packet, coordinates)
    }

    def registerFactory[P <: Packet](packetFactory: PacketFactory[P]): Unit = {
        val factory = packetFactory
        val ptClass = packetFactory.packetClass

        if (factories.contains(ptClass))
            throw new IllegalArgumentException(s"Packet '$ptClass' type is already registered !")

        factories.put(ptClass, factory)
        //notifier.onPacketTypeRegistered(ptClass, packetFactory) //TODO
    }


}
