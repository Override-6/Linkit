package fr.`override`.linkit.server

import fr.`override`.linkit.api.exception.{IllegalPacketWorkerLockException, RelayException}
import fr.`override`.linkit.api.packet.{Packet, PacketContainer, PacketCoordinates, TrafficHandler}
import fr.`override`.linkit.api.system.CloseReason

import scala.collection.mutable

class ServerTrafficHandler(server: RelayServer) extends TrafficHandler {


    private val registeredCollectors = mutable.Map.empty[Int, PacketContainer]
    //private val notifier = server.eventObserver.notifier

    override val relayID: String = server.identifier

    override def register(container: PacketContainer): Unit = {
        val id = container.identifier

        if (registeredCollectors.contains(id))
            throw new IllegalArgumentException(s"A packet collector with id '$id' is already registered")

        if (registeredCollectors.size > server.configuration.maxPacketContainerCacheSize)
            throw new RelayException("Maximum registered packet containers limit exceeded")

        registeredCollectors.put(id, container)
        //notifier.onPacketContainerRegistered(container)
    }

    override def unregister(id: Int, reason: CloseReason): Unit = {
        val opt = registeredCollectors.remove(id)
        //if (opt.isDefined)
            //notifier.onPacketContainerUnregistered(opt.get, reason)
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        registeredCollectors(coordinates.containerID).injectPacket(packet, coordinates)
    }

    override def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        sendPacket(packet, coords.containerID, coords.targetID)
    }

    override def sendPacket(packet: Packet, channelID: Int, targetID: String): Unit = {
        val connection = server.getConnection(targetID)
        if (connection == null)
            throw new IllegalArgumentException(s"Attempted to send packet to '$targetID', but this relay isn't connected on this server.")
        connection.sendPacket(packet, channelID)
    }

    override def close(reason: CloseReason): Unit = {
        registeredCollectors.values.foreach(_.close(reason))
        registeredCollectors.clear()
    }

    override def isRegistered(containerID: Int): Boolean = registeredCollectors.contains(containerID)

    override def checkThread(): Unit = {
        if (Thread.currentThread().getThreadGroup == server.packetWorkerThreadGroup)
            throw new IllegalPacketWorkerLockException("This packet worker thread was about to be locked by a monitor in order to wait packet reception")
    }
}
