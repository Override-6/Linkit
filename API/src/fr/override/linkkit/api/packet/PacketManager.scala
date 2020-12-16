package fr.`override`.linkkit.api.packet

import fr.`override`.linkkit.api.Relay
import fr.`override`.linkkit.api.`extension`.packet.PacketFactory
import fr.`override`.linkkit.api.exception.{PacketException, UnexpectedPacketException}
import fr.`override`.linkkit.api.packet.PacketUtils.wrap
import fr.`override`.linkkit.api.packet.fundamental._
import fr.`override`.linkkit.api.system.SystemPacket
import fr.`override`.linkkit.api.system.event.EventObserver.EventNotifier

import scala.collection.mutable


object PacketManager {
    val ChannelIDSeparator: Array[Byte] = "<channel>".getBytes
    val SenderSeparator: Array[Byte] = "<sender>".getBytes
    val TargetSeparator: Array[Byte] = "<target>".getBytes
}

class PacketManager(relay: Relay) { //Notifier is accessible from api to reduce parameter number in (A)SyncPacketChannel

    private val factories = mutable.LinkedHashMap.empty[Class[_ <: Packet], PacketFactory[_ <: Packet]]
    registerFundamentals()

    def register[P <: Packet](packetFactory: PacketFactory[P]): Unit = {
        val factory = packetFactory.asInstanceOf[PacketFactory[P]]
        val ptClass = packetFactory.packetClass.asInstanceOf[Class[P]]

        if (factories.contains(ptClass))
            throw new IllegalArgumentException(s"Packet '$ptClass' type is already registered !")

        factories.put(ptClass, factory)
        //notifier.onPacketTypeRegistered(ptClass, packetFactory) //TODO
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
        val packetBytes = factories(classOfP.asInstanceOf[Class[P]])
            .asInstanceOf[PacketFactory[P]]
            .decompose(packet)
        val bytes = PacketUtils.getCoordinatesBytes(coordinates) ++ packetBytes
        wrap(relay.securityManager.hashBytes(bytes))
    }

    def toBytes[D <: Packet](packet: D, coordinates: PacketCoordinates): Array[Byte] = {
        val packetClass = packet.getClass.asInstanceOf[Class[D]]
        toBytes(packetClass, packet, coordinates)
    }

    private def registerFundamentals(): Unit = {
        register(DataPacket)
        register(EmptyPacket.Factory)
        register(TaskInitPacket)
        register(ErrorPacket)
        register(SystemPacket)
    }


}
