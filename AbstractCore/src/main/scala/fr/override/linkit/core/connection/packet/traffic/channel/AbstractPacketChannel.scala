package fr.`override`.linkit.core.connection.packet.traffic.channel

import fr.`override`.linkit.internal.concurrency.relayWorkerExecution
import .PacketInjection
import fr.`override`.linkit.skull.connection.packet.traffic._
import fr.`override`.linkit.skull.internal.system.{CloseReason, ForbiddenIdentifierException}

import scala.collection.mutable

abstract class AbstractPacketChannel(scope: ChannelScope) extends PacketChannel with PacketInjectable {

    //protected but not recommended to use for implementations.
    //it could occurs of unexpected behaviors by the user.
    protected val writer: PacketWriter = scope.writer
    override val ownerID: String = writer.relayID
    override val identifier: Int = writer.identifier
    override val traffic: PacketTraffic = writer.traffic


    private val subChannels = mutable.Set.empty[SubInjectableContainer]

    @volatile private var closed = true

    override def close(reason: CloseReason): Unit = closed = true

    override def isClosed: Boolean = closed

    @relayWorkerExecution
    final override def inject(injection: PacketInjection): Unit = {
        val coordinates = injection.coordinates
        scope.assertAuthorised(coordinates.senderID)

        if (subInject(injection)) {
            handleInjection(injection)
        }
    }

    override def canInjectFrom(identifier: String): Boolean = scope.areAuthorised(identifier)

    override def subInjectable[C <: PacketInjectable](scopes: Array[String],
                                                      factory: PacketInjectableFactory[C],
                                                      transparent: Boolean): C = {
        if (scopes.exists(!scope.areAuthorised(_)))
            throw new ForbiddenIdentifierException("This sub injector requests to listen to an identifier that the parent does not support.")

        val subScope = ChannelScope.reserved(scopes: _*).apply(writer)
        register(subScope, factory, transparent)
    }

    override def subInjectable[C <: PacketInjectable](factory: PacketInjectableFactory[C], transparent: Boolean): C = {
        register(scope, factory, transparent)
    }

    private def register[C <: PacketInjectable](scope: ChannelScope,
                                                factory: PacketInjectableFactory[C],
                                                transparent: Boolean): C = {
        val channel = factory.createNew(scope)
        subChannels += SubInjectableContainer(channel, transparent)
        channel
    }

    @relayWorkerExecution
    def handleInjection(injection: PacketInjection): Unit

    protected case class SubInjectableContainer(subInjectable: PacketInjectable, transparent: Boolean)

    /**
     * @return true if the injection can be performed into this channel
     *         the boolean returned depends on the sub injectables.
     *         if one injectable is injected and is not transparent, this method will return false, so
     *         the current injectable could not handle packets for it.
     * */
    private def subInject(injection: PacketInjection): Boolean = {
        val coords = injection.coordinates

        val target = coords.targetID
        var authoriseInject = true
        for (container <- subChannels if container.subInjectable.canInjectFrom(target)) {
            //println(s"FOR container = ${container}")
            val injectable = container.subInjectable
            injectable.inject(injection)

            authoriseInject = authoriseInject && !container.transparent
        }
        authoriseInject
    }

}
