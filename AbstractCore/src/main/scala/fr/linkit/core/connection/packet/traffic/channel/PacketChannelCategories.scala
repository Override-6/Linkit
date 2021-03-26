/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.connection.packet.traffic.channel

import fr.linkit.api.connection.packet.traffic.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory, PacketInjection}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.core.connection.packet.UnexpectedPacketException
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.connection.packet.traffic.PacketInjections

import scala.collection.mutable

class PacketChannelCategories(scope: ChannelScope) extends AbstractPacketChannel(scope) {

    private val categories = mutable.Map.empty[String, PacketInjectable]

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val packets = injection.getPackets
        val coordinates = injection.coordinates
        packets.foreach {
            case WrappedPacket(category, subPacket) =>
                val injection = PacketInjections.unhandled(coordinates, subPacket)
                categories.get(category).foreach(_.inject(injection))

            case packet => throw UnexpectedPacketException(s"Received unexpected packet $packet")
        }
    }

    def createCategory[C <: PacketInjectable](name: String, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        //TODO if (categories.contains(name)) <- Has need to be removed due to the client/server reconnection.
        //         throw new IllegalArgumentException(s"The category '$name' already exists for this categorised channel")

        categories.getOrElseUpdate(name, {
            val writer = traffic.newWriter(identifier, WrappedPacket(name, _))
            val channel = factory.createNew(scopeFactory(writer))
            categories.put(name, channel)
            channel
        }).asInstanceOf[C]
    }

}

object PacketChannelCategories extends PacketInjectableFactory[PacketChannelCategories] {
    override def createNew(scope: ChannelScope): PacketChannelCategories = {
        new PacketChannelCategories(scope)
    }
}