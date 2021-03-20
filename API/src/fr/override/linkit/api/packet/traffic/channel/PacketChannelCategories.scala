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

package fr.`override`.linkit.api.packet.traffic.channel

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.packet.traffic.ChannelScope.ScopeFactory
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory, PacketInjections}

import scala.collection.mutable

class PacketChannelCategories(scope: ChannelScope) extends AbstractPacketChannel(scope) {

    private val categories = mutable.Map.empty[String, PacketInjectable]

    @relayWorkerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        val packets = injection.getPackets
        val coordinates = injection.coordinates
        packets.foreach {
            case WrappedPacket(category, subPacket) =>
                val injection = PacketInjections.unhandled(coordinates, subPacket)
                categories.get(category).foreach(_.inject(injection))

            case packet => throw new UnexpectedPacketException(s"Received unexpected packet $packet")
        }
    }

    def createCategory[C <: PacketInjectable](name: String, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        if (categories.contains(name))
            throw new IllegalArgumentException(s"The category '$name' already exists for this categorised channel")

        val writer = traffic.newWriter(identifier, WrappedPacket(name, _))
        val channel = factory.createNew(scopeFactory(writer))
        categories.put(name, channel)
        channel
    }

}

object PacketChannelCategories extends PacketInjectableFactory[PacketChannelCategories] {
    override def createNew(scope: ChannelScope): PacketChannelCategories = {
        new PacketChannelCategories(scope)
    }
}