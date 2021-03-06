package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.concurrency.PacketWorkerThread
import fr.`override`.linkit.api.exception.{ClosedException, ConflictException, RelayException}
import fr.`override`.linkit.api.packet.traffic.ChannelScope.ScopeFactory
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketTraffic}
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
    private val holders = mutable.Map.empty[Int, ScopesHolder]
    @volatile private var closed = false

    private val lostInjections = mutable.Map.empty[Int, ListBuffer[PacketInjection]]

    override def getInjectable[C <: PacketInjectable : ClassTag](id: Int,
                                                                    scopeFactory: ScopeFactory[_ <: ChannelScope],
                                                                    factory: PacketInjectableFactory[C]): C = {
        val scope = scopeFactory(newWriter(id))
        val holderOpt = holders.get(id)
        if (holderOpt.isDefined) {
            val holder = holderOpt.get
            val attempt = holder.tryRetrieveInjectable(scope)
            if (attempt.isDefined) {
                return attempt.get
            }

            if (holder.canConflict(scope)) {
                throw new ConflictException("This scope can conflict with other scopes that are registered within this injectable identifier")
            }
        }

        completeCreation(scope, factory)
    }

    override def canConflict(identifier: Int, scope: ChannelScope): Boolean = {
        holders
                .get(identifier)
                .exists(_.canConflict(scope))
    }

    private def completeCreation[C <: PacketInjectable](scope: ChannelScope, factory: PacketInjectableFactory[C]): C = {
        val channel = factory.createNew(scope)
        register(scope, channel)
        channel
    }

    private def register(scope: ChannelScope, dedicated: PacketInjectable): Unit = {
        ensureOpen()

        val id = dedicated.identifier
        init(dedicated)

        holders.getOrElseUpdate(id, ScopesHolder(id)).register(scope, dedicated)
    }

    private def init(injectable: PacketInjectable): Unit = {

        if (holders.size > config.maxPacketContainerCacheSize) {
            throw new RelayException("Maximum registered packet containers limit exceeded")
        }

        val id = injectable.identifier

        //Will inject every lost packets
        lostInjections
                .get(id)
                .foreach(_.foreach(injectable.inject))
        lostInjections.remove(id)
    }

    private def getInjectables(identifier: Int, target: String): Iterable[PacketInjectable] = {
        val opt = holders.get(identifier)

        if (opt.isEmpty)
            return Iterable()


        opt.get.getInjectables(target)
    }

    override def close(reason: CloseReason): Unit = {
        holders.values
                .foreach(_.close(reason))
        holders.clear()
        closed = true
    }

    override def isClosed: Boolean = closed

    protected def ensureOpen(): Unit = {
        if (closed)
            throw new ClosedException("This Traffic handler is closed")
    }

    override def handleInjection(injection: PacketInjection): Unit = {
        if (injection.mayNotHandle) {
            //println(s"Injection handling has been rejected for thread ${Thread.currentThread()}")
            //println(s"The injection is already handled by thread ${injection.handlerThread}")
            //injection.handlerThread.getStackTrace.foreach(println)
            return
        }

        val coordinates = injection.coordinates
        PacketWorkerThread.checkNotCurrent()
        ensureOpen()

        val id = coordinates.injectableID

        val sender = coordinates.senderID
        val injectables = getInjectables(id, sender)
        if (injectables.isEmpty) {
            lostInjections.getOrElseUpdate(id, ListBuffer.empty) += injection
            return
        }
        injectables.foreach(_.inject(injection))
    }

    protected case class ScopesHolder(identifier: Int) extends JustifiedCloseable {

        private val cache = mutable.Set.empty[(ChannelScope, PacketInjectable)]
        private var closed = false

        override def close(reason: CloseReason): Unit = {
            for (tuple <- cache if tuple._2.isOpen) try {
                tuple._2.close()
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
            cache.clear()
            closed = true
        }

        override def isClosed: Boolean = closed

        def canConflict(scope: ChannelScope): Boolean = {
            cache.exists(_._1.canConflictWith(scope))
        }

        def isAuthorised(targetID: String): Boolean = {
            cache.exists(_._1.areAuthorised(targetID))
        }

        def getInjectables(target: String): Seq[PacketInjectable] = {
            cache
                    .filter(_._1.areAuthorised(target))
                    .map(_._2)
                    .toSeq
        }

        def tryRetrieveInjectable[I <: PacketInjectable : ClassTag](scope: ChannelScope): Option[I] = {
            val injectableClass = classTag[I].runtimeClass
            cache.find(tuple => tuple._1 == scope && tuple._2.getClass == injectableClass)
                    .map(_._2)
                    .asInstanceOf[Option[I]]
        }

        def register(scope: ChannelScope, injectable: PacketInjectable): Unit = {
            cache += ((scope, injectable))
        }
    }


}