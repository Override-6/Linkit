package fr.overridescala.linkkit.api.packet

import fr.overridescala.linkkit.api.system.Reason
import fr.overridescala.linkkit.api.system.event.EventObserver.EventNotifier

import scala.collection.mutable

class SimpleTrafficHandler(val notifier: EventNotifier,
                           socket: DynamicSocket,
                           override val relayID: String,
                           packetManager: PacketManager) extends TrafficHandler {

    private val registeredContainers = mutable.Map.empty[Int, PacketContainer]

    override def register(container: PacketContainer): Unit = {
        val id = container.identifier
        println("registered " + id)
        if (registeredContainers.contains(id))
            throw new IllegalArgumentException(s"A packet container with id '$id' is already registered to this traffic handler")

        registeredContainers.put(id, container)
        notifier.onPacketContainerRegistered(container)
    }


    override def unregister(id: Int, reason: Reason): Unit = {
        println("unregistering " + id)
        val opt = registeredContainers.remove(id)
        if (opt.isDefined)
            notifier.onPacketContainerUnregistered(opt.get, reason)
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        println(s"packet = ${packet}")
        println(s"coordinates = ${coordinates}")
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