/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.core.connection.network.cache.puppet

import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.connection.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.{Packet, PacketAttributesPresence}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.connection.packet.traffic.channel.request.{RequestPacket, RequestPacketChannel, ResponseSubmitter}

import scala.collection.mutable.ListBuffer

class ObjectChip[S <: Serializable](channel: RequestPacketChannel,
                                    presence: PacketAttributesPresence,
                                    id: Long,
                                    val owner: String,
                                    val puppet: S) {

    private val ownerScope = prepareScope(ChannelScopes.retains(owner))
    private val bcScope    = prepareScope(ChannelScopes.discardCurrent)

    private val puppetModifications = ListBuffer.empty[(String, Any)]
    private val puppeteer           = Puppeteer[S](puppet)

    def sendInvoke(methodName: String, args: Any*): Any = {
        channel.makeRequest(ownerScope)
                .addPacket(ObjectPacket((methodName, Array(args: _*))))
                .submit()
                .detach()

    }

    def addFieldUpdate(fieldName: String, newValue: Any): Unit = {
        AppLogger.vDebug(s"Field '$fieldName' of object $puppet took value $newValue")
        if (puppeteer.autoFlush)
            flushModification((fieldName, newValue))
        else puppetModifications += ((fieldName, newValue))
    }

    def updatePuppet(newVersion: Serializable): Unit = {
        puppeteer.updateAllFields(newVersion)
    }

    def sendUpdatePuppet(newVersion: Serializable): Unit = {
        puppeteer.accessor.foreachSharedFields(field => addFieldUpdate(field.getName, field.get(newVersion)))
    }

    private[puppet] def handleRequest(packet: Packet, submitter: ResponseSubmitter): Unit = {
        packet match {
            case ObjectPacket((fieldName: String, value: Any)) =>
                puppeteer.updateField(fieldName, value)

            case ObjectPacket((methodName: String, args: Array[Any])) =>
                var result: Serializable = null
                if (puppeteer.canCallMethod(methodName)) {
                    result = puppeteer.callMethod(methodName, args)
                }
                submitter
                        .addPacket(ObjectPacket(result))
                        .submit()
        }
    }

    def flush(): this.type = {
        puppetModifications.foreach(flushModification)
        puppetModifications.clear()
        this
    }

    private def flushModification(mod: (String, Any)): Unit = {
        channel.makeRequest(bcScope)
                .addPacket(ObjectPacket(mod))
                .submit()
                .detach()
    }

    private def prepareScope(factory: ScopeFactory[_ <: ChannelScope]): ChannelScope = {
        val writer = channel.traffic.newWriter(channel.identifier)
        val scope  = factory.apply(writer)
        presence.drainAllDefaultAttributes(scope)
        scope
                .addDefaultAttribute("owner", owner)
                .addDefaultAttribute("id", id)
    }

}
