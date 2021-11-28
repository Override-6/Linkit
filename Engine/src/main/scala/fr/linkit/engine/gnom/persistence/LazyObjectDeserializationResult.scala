/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.gnom.persistence.ObjectDeserializationResult
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket

import java.nio.ByteBuffer
import scala.reflect.{ClassTag, classTag}

class LazyObjectDeserializationResult(override val buff: ByteBuffer,
                                      override val coords: PacketCoordinates)
                                     (forEachObjects: (AnyRef => Unit) => Unit) extends ObjectDeserializationResult {

    private lazy val cache : Array[AnyRef]    = createCache()
    private var attributes0: PacketAttributes = _
    private var packet0    : Packet           = _

    override def attributes: PacketAttributes = {
        if (attributes0 == null)
            throw new NotDeserializedException("Object is not deserialized")
        attributes0
    }

    override def packet: Packet = {
        if (attributes0 == null)
            throw new NotDeserializedException("Object is not deserialized")
        packet0
    }

    override def makeDeserialization(): Unit = {
        attributes0 = extract[PacketAttributes](SimplePacketAttributes.empty)
        packet0 = extract[Packet](EmptyPacket).prepare()
    }

    private def extract[T <: Serializable : ClassTag](orElse: => T): T = {
        val clazz       = classTag[T].runtimeClass
        val coordsIndex = cache.indexWhere(o => o != null && clazz.isAssignableFrom(o.getClass))

        if (coordsIndex < 0) {
            val alternative = orElse
            if (alternative == null)
                throw new MalFormedPacketException(buff, s"Received unexpected null item into packet array")
            else return alternative
        }
        val result = cache(coordsIndex) match {
            case e: T => e
        }
        result
    }

    private def createCache(): Array[AnyRef] = {
        AppLogger.debug("Deserializing Packet and Attributes...")
        val cache = new Array[AnyRef](2)
        forEachObjects {
            case attributes: PacketAttributes => cache(0) = attributes
            case packet: Packet               => cache(1) = packet
        }
        AppLogger.debug("Deserialization done.")
        cache
    }

}