package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.concurrency.PacketWorkerThread
import fr.`override`.linkit.api.exception.{ClosedException, RelayException}
import fr.`override`.linkit.api.packet.traffic.dedicated.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.traffic.global.{PacketCollector, PacketCollectorFactory}
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.config.RelayConfiguration
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}
import org.jetbrains.annotations.NotNull

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}
import scala.util.control.NonFatal

abstract class AbstractPacketTraffic(@NotNull config: RelayConfiguration,
                                     @NotNull private val ownerId: String) extends PacketTraffic {

    override val relayID: String = ownerId
    private val registeredInjectables = mutable.Map.empty[Int, InjectableGroup]
    @volatile private var closed = false

    @deprecated("Find a better solution; cache every lost packets may be very heavy. (take a look at Relay.scala TODO list)")
    private val lostPackets = mutable.Map.empty[(Int, String), ListBuffer[(Packet, PacketCoordinates)]]

    override def openChannel[A <: PacketChannel : ClassTag](injectableID: Int, targetID: String, factory: PacketChannelFactory[A]): A = {
        if (isRegistered(injectableID, targetID)) {
            return getInjectableOfType[A](injectableID, targetID)
        }
        val channel = factory.createNew(newWriter(injectableID, p => p), targetID)
        register(channel)
        channel
    }

    override def openCollector[A <: PacketCollector : ClassTag](injectableID: Int, factory: PacketCollectorFactory[A]): A = {
        if (isRegistered(injectableID, null)) {
            return getInjectableOfType[A](injectableID, null)
        }

        val channel = factory.createNew(newWriter(injectableID, p => p))
        register(channel)
        channel
    }

    override def register(dedicated: DedicatedPacketInjectable): Unit = {
        ensureOpen()

        val id = dedicated.identifier
        val dedicatedTarget = dedicated.connectedID

        init(dedicated, dedicatedTarget)

        registeredInjectables.getOrElseUpdate(id, InjectableGroup(id)).put(dedicated)
    }

    override def register(global: GlobalPacketInjectable): Unit = {
        ensureOpen()

        val id = global.identifier

        init(global, null)

        registeredInjectables.getOrElseUpdate(id, InjectableGroup(id)).put(global)
    }

    private def init(injectable: PacketInjectable, target: String): Unit = {

        if (registeredInjectables.size > config.maxPacketContainerCacheSize) {
            throw new RelayException("Maximum registered packet containers limit exceeded")
        }

        val id = injectable.identifier

        //Does work as expected; this is the entire port that is tested and/or removed, where this is the old closed injectable that MUST be replaced
        if (isRegistered(id, target)) {
            val injectable = registeredInjectables(id)
            if (injectable.isClosed)
                registeredInjectables.remove(id)
            else {
                throw new IllegalArgumentException(s"A packet injectable with id '$id' is already registered to this traffic handler")
            }
        }

        //Will inject every lost packets
        lostPackets.get((id, target))
                .foreach(_.foreach(t => injectable.injectPacket(t._1, t._2)))
        lostPackets.remove((id, target))
    }

    private def getInjectableOfType[A: ClassTag](identifier: Int, target: String): A = {
        getInjectable(identifier, target).orNull match {
            case registeredInjectable: A => registeredInjectable
            case other => throw new UnsupportedOperationException(s"Attempted to retrieve injectable (id: $identifier) with requested kind : ${classTag[A].runtimeClass}, but found ${other.getClass}")
        }
    }

    private def getInjectable(identifier: Int, target: String): Option[PacketInjectable] = {
        registeredInjectables
                .get(identifier)
                .flatMap(_.getInjectable(target))
    }

    override def isRegistered(identifier: Int, boundTarget: String): Boolean = {
        registeredInjectables.get(identifier).exists(_.isRegistered(boundTarget))
    }

    override def close(reason: CloseReason): Unit = {
        registeredInjectables.values
                .foreach(_.close(reason))
        registeredInjectables.clear()
        closed = true
    }

    override def isClosed: Boolean = closed

    protected def ensureOpen(): Unit = {
        if (closed)
            throw new ClosedException("This Traffic handler is closed")
    }

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        //println(s"INJECTING PACKET : $packet With coordinate : $coordinates (${Thread.currentThread()})")
        PacketWorkerThread.checkNotCurrent()
        ensureOpen()

        val id = coordinates.injectableID
        val sender = coordinates.senderID
        if (!isRegistered(id, sender)) {
            lostPackets.getOrElseUpdate((id, sender), ListBuffer.empty) += ((packet, coordinates))
            return
        }
        registeredInjectables(id)
                .inject(packet, coordinates)
    }

    protected case class InjectableGroup(identifier: Int) extends JustifiedCloseable {
        private val cache = mutable.Map.empty[String, PacketInjectable]
        private var isGlobal = false
        private var closed = false

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

        def getInjectable(target: String): Option[PacketInjectable] = {
            if (isGlobal)
                Option(cache.values.head)
            else Option(cache.get(target).orNull)
        }

        def isRegistered(target: String): Boolean = {
            isGlobal || cache.contains(target)
        }

        def put(global: GlobalPacketInjectable): Unit = {
            ensureNotGlobal()
            cache.clear()
            isGlobal = true
            cache += ((null, global))
        }

        def put(dedicated: DedicatedPacketInjectable): Unit = {
            ensureNotGlobal()
            cache += ((dedicated.connectedID, dedicated))
        }

        private def ensureNotGlobal(): Unit = {
            if (isGlobal)
                throw new IllegalStateException(s"Attempted to register a PacketInjectable into a global context")
        }

        def inject(packet: Packet, coords: PacketCoordinates): Unit = {
            if (isGlobal)
                if (cache.size == 1)
                    cache.values.head.injectPacket(packet, coords)
                else throw new IllegalStateException("Attempted to inject a packet into a global packet injectable but multiples injectable where found.")
            else cache.get(coords.senderID).foreach(_.injectPacket(packet, coords))
        }
    }


}