package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.{IllegalPacketWorkerLockException, RelayException}
import fr.`override`.linkit.api.system.CloseReason

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


    override def unregister(id: Int, reason: CloseReason): Unit = {
        val opt = registeredContainers.remove(id)
        if (opt.isDefined)
            notifier.onPacketContainerUnregistered(opt.get, reason)
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        registeredContainers(coordinates.containerID)
            .injectPacket(packet, coordinates)
    }

    override def sendPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        if (socket.isOpen) {
            socket.write(packetManager.toBytes(packet, coordinates))
            notifier.onPacketSent(packet, coordinates)
        }
    }

    override def sendPacket(packet: Packet, identifier: Int, targetID: String): Unit = {
        sendPacket(packet, PacketCoordinates(identifier, targetID, relayID))
    }

    override def isRegistered(containerID: Int): Boolean = registeredContainers.contains(containerID)

    override def close(reason: CloseReason): Unit = {
        for ((_, channel) <- registeredContainers) {
            channel.close(reason)
        }
    }

    override def checkThread(): Unit = {
        if (Thread.currentThread().getThreadGroup == relay.packetWorkerThreadGroup)
            throw new IllegalPacketWorkerLockException("This packet worker thread was about to be locked by a monitor in order to wait packet reception")
    }

}