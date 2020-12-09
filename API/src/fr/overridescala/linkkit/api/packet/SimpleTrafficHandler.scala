package fr.overridescala.linkkit.api.packet

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.api.exceptions.RelayException
import fr.overridescala.linkkit.api.system.Reason
import fr.overridescala.linkkit.api.system.event.EventObserver.EventNotifier

import scala.collection.mutable

class SimpleTrafficHandler(relay: Relay,
                           socket: DynamicSocket) extends TrafficHandler {

    private val packetManager = relay.packetManager
    private val registeredContainers = mutable.Map.empty[Int, PacketContainer]
    private val notifier = relay.eventObserver.notifier

    override val relayID: String = relay.identifier

    override def register(container: PacketContainer): Unit = {
        val id = container.identifier

        if (registeredContainers.contains(id))
            throw new IllegalArgumentException(s"A packet container with id '$id' is already registered to this traffic handler")

        if (registeredContainers.size > relay.configuration.maxPacketContainerCacheSize)
            throw new RelayException("Maximum registered packet containers limit exceeded")

        registeredContainers.put(id, container)
        notifier.onPacketContainerRegistered(container)
    }


    override def unregister(id: Int, reason: Reason): Unit = {
        val opt = registeredContainers.remove(id)
        if (opt.isDefined)
            notifier.onPacketContainerUnregistered(opt.get, reason)
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        registeredContainers(coordinates.containerID)
                .injectPacket(packet, coordinates)
    }

    override def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        socket.write(packetManager.toBytes(packet, coords))
        notifier.onPacketSent(packet, coords)
    }

    override def sendPacket(packet: Packet, identifier: Int, targetID: String): Unit = {
        sendPacket(packet, PacketCoordinates(identifier, targetID, relayID))
    }

    def notifyPacketUsed(packet: Packet, coordinates: PacketCoordinates): Unit =
        notifier.onPacketUsed(packet, coordinates)

    override def isTargeted(containerID: Int): Boolean = registeredContainers.contains(containerID)

    override def close(reason: Reason): Unit = {
        for ((_, channel) <- registeredContainers) {
            channel.close(reason)
        }
    }

}