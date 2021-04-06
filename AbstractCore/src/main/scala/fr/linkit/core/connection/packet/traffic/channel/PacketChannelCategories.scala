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
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.core.connection.packet.UnexpectedPacketException
import fr.linkit.core.connection.packet.traffic.channel.PacketChannelCategories.nextChannelNumber
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

class PacketChannelCategories(@Nullable parent: PacketChannel, scope: ChannelScope) extends AbstractPacketChannel(parent, scope) {

    private val categories    = mutable.Map.empty[String, PacketInjectable]
    private val channelNumber = nextChannelNumber

    @workerExecution
    override def handleInjection(injection: PacketInjection): Unit = {
        injection.attachPin {
            (packet, attr) => {
                val attribute = attr.getAttribute[String]("cat")
                if (attribute.isEmpty) throw UnexpectedPacketException(s"Received unexpected packet $packet")
                val categoryName = attribute.get
                categories.get(categoryName).fold(throw new NoSuchElementException(s"Category '$categoryName' does not exists into PacketChannelCategories$channelNumber.")) {
                    category => category.inject(injection)
                }
            }
        }
    }

    def createCategory[C <: PacketInjectable](name: String, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        //TODO if (categories.contains(name)) <- Has need to be removed due to the client/server reconnection.
        //         throw new IllegalArgumentException(s"The category '$name' already exists for this categorised channel")

        categories.getOrElseUpdate(name, {
            val writer  = traffic.newWriter(identifier)
            val channel = factory.createNew(this, scopeFactory(writer))
            channel.addDefaultAttribute("cat", name)
            categories.put(name, channel)
            channel
        }).asInstanceOf[C]
    }

}

object PacketChannelCategories extends PacketInjectableFactory[PacketChannelCategories] {

    @volatile private var channelNumber: Int = 0

    private def nextChannelNumber: Int = {
        channelNumber += 1
        channelNumber
    }

    override def createNew(parent: PacketChannel, scope: ChannelScope): PacketChannelCategories = {
        new PacketChannelCategories(parent: PacketChannel, scope)
    }
}