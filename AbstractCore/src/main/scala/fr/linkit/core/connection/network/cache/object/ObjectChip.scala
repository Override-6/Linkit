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

package fr.linkit.core.connection.network.cache.`object`

import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.connection.packet.traffic.channel.request.{RequestBundle, RequestPacket, RequestPacketChannel}

import scala.collection.mutable.ListBuffer

class ObjectChip[S <: Serializable](channel: RequestPacketChannel,
                                               id: Long,
                                               val owner: String,
                                               val puppet: S) {

    private val ownerScope          = prepareOwnerScope()
    private val puppetModifications = ListBuffer.empty[(String, Any)]
    private val puppeteer           = Puppeteer[S](puppet)

    def sendInvoke(methodName: String, args: Any*): Any = {
        channel.makeRequest(ownerScope)
                .addPacket(ObjectPacket((methodName, Array(args: _*))))
                .submit()
                .detach()

    }

    def addFieldUpdate(fieldName: String, newValue: Any): Unit = {
        if (puppeteer.autoFlush)
            flushModification((fieldName, newValue))
        else puppetModifications += ((fieldName, newValue))
    }

    def updatePuppet(clone: Serializable): Unit = {
        puppeteer.updateAllFields(clone)
    }

    private[`object`] def handleBundle(bundle: RequestBundle): Unit = {
        bundle.packet match {
            case RequestPacket(_, Array(ObjectPacket((fieldName: String, value: Any)))) =>
                puppeteer.updateField(fieldName, value)

            case RequestPacket(_, Array(ObjectPacket((methodName: String, args: Array[Any])))) =>
                var result: Serializable = null
                if (puppeteer.canCallMethod(methodName)) {
                    result = puppeteer.callMethod(methodName, args)
                }
                bundle.responseSubmitter
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
        channel.makeRequest(ChannelScopes.broadcast)
                .addPacket(ObjectPacket(mod))
                .submit()
                .detach()
    }

    private def prepareOwnerScope(): ChannelScope = {
        val writer = channel.traffic.newWriter(channel.identifier)
        val scope  = ChannelScopes.reserved(owner).apply(writer)
        scope
                .addDefaultAttribute("owner", owner)
                .addDefaultAttribute("id", id)
    }

}
