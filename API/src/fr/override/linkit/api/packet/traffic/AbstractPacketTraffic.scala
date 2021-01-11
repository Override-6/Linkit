package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.{ClosedException, IllegalPacketWorkerLockException, RelayException}
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.CloseReason

import scala.collection.mutable
import scala.util.control.NonFatal

abstract class AbstractPacketTraffic(relay: Relay, private val ownerId: String) extends PacketTraffic {

    override val ownerID: String = ownerId
    private val registeredInjectables = mutable.Map.empty[Int, PacketInjectable]
    @volatile private var closed = false

    override def register(injectable: PacketInjectable): Unit = {
        ensureOpen()

        val id = injectable.identifier
        if (registeredInjectables.size > relay.configuration.maxPacketContainerCacheSize)
            throw new RelayException("Maximum registered packet containers limit exceeded")

        if (registeredInjectables.contains(id)) {
            val injectable = registeredInjectables(id)
            if (injectable.isClosed)
                registeredInjectables.remove(id)
            else throw new IllegalArgumentException(s"A packet injectable with id '$id' is already registered to this traffic handler")
        }

        registeredInjectables.put(id, injectable)
    }

    protected def ensureOpen(): Unit = {
        if (closed)
            throw new ClosedException("This Traffic handler is closed")
    }

    override def unregister(id: Int, reason: CloseReason): Unit = {
        ensureOpen()

        val opt = registeredInjectables.remove(id)
        if (opt.isDefined) {
            val injectable = opt.get
            if (!injectable.isClosed)
                injectable.close(reason)
        }
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        ensureOpen()
        registeredInjectables(coordinates.injectableID)
                .injectPacket(packet, coordinates)
    }

    override def writePacket(packet: Packet, identifier: Int, targetID: String): Unit = {
        ensureOpen()
        writePacket(packet, PacketCoordinates(identifier, targetID, relay.identifier))
    }

    override def writePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        if (coordinates.targetID == relay.identifier)
            injectPacket(packet, coordinates)
        else send(packet, coordinates)
    }

    override def isRegistered(containerID: Int): Boolean = registeredInjectables.contains(containerID)


    override def close(reason: CloseReason): Unit = {
        for ((_, channel) <- registeredInjectables if channel.isClosed) try {
            channel.close(reason)
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
        registeredInjectables.clear()
        closed = true
    }

    override def checkThread(): Unit = {
        if (Thread.currentThread().getThreadGroup == relay.packetWorkerThreadGroup)
            throw new IllegalPacketWorkerLockException("This packet worker thread was about to be locked by a monitor in order to wait packet reception")
    }

    override def isClosed: Boolean = closed

    def send(packet: Packet, coordinates: PacketCoordinates): Unit
}