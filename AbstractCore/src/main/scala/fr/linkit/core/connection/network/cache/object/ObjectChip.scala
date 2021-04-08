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

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.packet.Bundle
import fr.linkit.core.connection.network.cache.{AbstractSharedCache, AsyncSenderSyncReceiver}
import fr.linkit.core.connection.packet.UnexpectedPacketException
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.traffic.channel.request.{RequestBundle, RequestPacket, RequestPacketChannel}

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

class ObjectChip[S <: Serializable : ClassTag](handler: SharedCacheManager,
                                               id: Long,
                                               channel: RequestPacketChannel,
                                               override val family: String,
                                               val puppet: S)
        extends AbstractSharedCache[S](handler, id, channel) {

    private val puppetModifications = ListBuffer.empty[(String, Any)]
    private val puppeteer           = Puppeteer(puppet)
    private var modCount            = 0

    override var autoFlush: Boolean = true

    override protected def setCurrentContent(content: Array[S]): Unit = {
        val clone = content(0)
        puppeteer.updateAll(clone)
    }

    override def handleBundle(bundle: Bundle): Unit = {
        bundle match {
            case rb: RequestBundle => handleRequestBundle(rb)
            case _                 => throw UnexpectedPacketException("Expected RequestBundle for ObjectChip.")
        }
    }

    override def currentContent: Array[Any] = Array(puppet)

    override def flush(): this.type = {
        puppetModifications.foreach(flushModification)
        puppetModifications.clear()
        this
    }

    override def modificationCount(): Int = {
        modCount
    }

    private def handleRequestBundle(requestBundle: RequestBundle): Unit = {
        requestBundle.packet match {
            case RequestPacket(_, Array(ObjectPacket((fieldName: String, value: Any)))) =>
                puppeteer.updateField(fieldName, value)

            case RequestPacket(_, Array(ObjectPacket((methodName: String, args: Array[Any])))) =>
                var result: Serializable = null
                if (puppeteer.canCallMethod(methodName)) {
                    result = puppeteer.callMethod(methodName, args)
                }
                requestBundle.responseSubmitter
                        .addPacket(ObjectPacket(result))
                        .submit()
        }
    }

    private def flushModification(mod: (String, Any)): Unit = {

    }

}
