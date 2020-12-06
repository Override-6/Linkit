package fr.overridescala.linkkit.server

import fr.overridescala.linkkit.api.packet.{Packet, PacketContainer, PacketCoordinates, TrafficHandler}
import fr.overridescala.linkkit.api.system.Reason

import scala.collection.mutable

class ServerTrafficHandler(server: RelayServer) extends TrafficHandler {


    private val registeredCollectors = mutable.Map.empty[Int, PacketContainer]
    private val notifier = server.eventObserver.notifier

    override val relayID: String = server.identifier

    override def register(container: PacketContainer): Unit = {
        val id = container.identifier

        if (registeredCollectors.contains(id))
            throw new IllegalArgumentException(s"A packet collector with id '$id' is already registered")

        registeredCollectors.put(id, container)
        notifier.onPacketContainerRegistered(container)
    }

    override def unregister(id: Int, reason: Reason): Unit = {
        val opt = registeredCollectors.remove(id)
        if (opt.isDefined)
            notifier.onPacketContainerUnregistered(opt.get, reason)
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        registeredCollectors(coordinates.containerID).injectPacket(packet, coordinates)
    }

    override def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        sendPacket(packet, coords.containerID, coords.targetID)
    }

    override def sendPacket(packet: Packet, channelID: Int, targetID: String): Unit = {
        server.getConnection(targetID).sendPacket(packet, channelID)
    }

    override def close(reason: Reason): Unit = {
        registeredCollectors.values.foreach(_.close(reason))
        registeredCollectors.clear()
    }

    override def isTargeted(containerID: Int): Boolean = registeredCollectors.contains(containerID)
}
