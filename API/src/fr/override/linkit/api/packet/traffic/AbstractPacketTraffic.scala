package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.{ClosedException, IllegalPacketWorkerLockException, RelayException}
import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.collector.{PacketCollector, PacketCollectorFactory}
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

abstract class AbstractPacketTraffic(relay: Relay, private val ownerId: String) extends PacketTraffic {

    override val ownerID: String = ownerId
    private val registeredInjectables = mutable.Map.empty[Int, InjectablePort]
    @volatile private var closed = false

    @deprecated("Find a better solution; cache every lost packets may be very heavy. (take a look at Relay.scala TODO list)")
    private val lostPackets = mutable.Map.empty[(Int, String), ListBuffer[(Packet, PacketCoordinates)]]

    override def register(dedicated: DedicatedPacketInjectable): Unit = {
        ensureOpen()

        val id = dedicated.identifier
        val dedicatedTarget = dedicated.connectedID

        if (registeredInjectables.size > relay.configuration.maxPacketContainerCacheSize) {
            throw new RelayException("Maximum registered packet containers limit exceeded")
        }

        if (isRegistered(id, dedicatedTarget)) {
            val injectable = registeredInjectables(id)
            if (injectable.isClosed)
                registeredInjectables.remove(id)
            else {
                throw new IllegalArgumentException(s"A packet injectable with id '$id' is already registered to this traffic handler")
            }
        }

        //Will inject every lost packets

        lostPackets.get((id, dedicatedTarget))
                .foreach(_.foreach(t => dedicated.injectPacket(t._1, t._2)))
        lostPackets.remove((id, dedicatedTarget))

        registeredInjectables.getOrElseUpdate(id, InjectablePort(id)).putGlobal()
    }

    override def openChannel[C <: PacketChannel](channelId: Int, targetID: String, factory: PacketChannelFactory[C]): C = {
        if (isRegistered(channelId)) {
            registeredInjectables(channelId) match {
                case registeredChannel: C => return registeredChannel
                case other => throw new UnsupportedOperationException(s"Attempted to retrieve channel (id: $channelId) with requested kind : ${factory.channelClass}, but found ${other.getClass}")
            }
        }
        val channel = factory.createNew(this, channelId, targetID)
        register(channel)
        channel
    }

    override def openCollector[C <: PacketCollector](channelId: Int, factory: PacketCollectorFactory[C]): C = {
        if (isRegistered(channelId)) {
            registeredInjectables(channelId) match {
                case registeredChannel: C => return registeredChannel
                case other => throw new UnsupportedOperationException(s"Attempted to retrieve channel (id: $channelId) with requested kind : ${factory.collectorClass}, but found ${other.getClass}")
            }
        }
        val channel = factory.createNew(this, channelId)
        register(channel)
        channel
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        ensureOpen()
        val id = coordinates.injectableID
        if (!isRegistered(id, coordinates.senderID)) {
            lostPackets.getOrElseUpdate(id, ListBuffer.empty) += ((packet, coordinates))
            return
        }
        registeredInjectables(id)
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

    override def isRegistered(identifier: Int, boundTarget: String): Boolean = {
        registeredInjectables.get(identifier).exists(_.isRegistered(boundTarget))
    }

    override def close(reason: CloseReason): Unit = {
        registeredInjectables.values.foreach(_.close(reason))
        registeredInjectables.clear()
        closed = true
    }

    override def checkThread(): Unit = {
        if (Thread.currentThread().getThreadGroup == relay.packetWorkerThreadGroup)
            throw new IllegalPacketWorkerLockException("This packet worker thread was about to be locked by a monitor in order to wait packet reception")
    }

    override def isClosed: Boolean = closed

    protected def ensureOpen(): Unit = {
        if (closed)
            throw new ClosedException("This Traffic handler is closed")
    }

    protected def send(packet: Packet, coordinates: PacketCoordinates): Unit

    private case class InjectablePort(identifier: Int) extends JustifiedCloseable {
        private val cache = mutable.Map.empty[String, PacketInjectable]
        private var global = false
        private var closed = false

        def isRegistered(target: String): Boolean = {
            cache.contains(null) || cache.contains(target)
        }

        def put(global: GlobalPacketInjectable): Unit = {
            ensureNotGlobal()
            cache.clear()
            global = true
            cache += ((null, dedicated))
        }

        def put(dedicated: DedicatedPacketInjectable): Unit = {
            ensureNotGlobal()
            cache += ((dedicated.connectedID, dedicated))
        }

        private def ensureNotGlobal(): Unit = {
            if (global)
                throw new IllegalStateException(s"Attempted to register a PacketInjectable into a global context")
        }

        override def close(reason: CloseReason): Unit = {
            for (injectable <- cache.values if injectable.isOpen) try {
                injectable.close()
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
            cache.clear()
            closed = true
        }

        override def isClosed: Boolean = closed
    }

}