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

package fr.linkit.engine.connection.packet.persistence

import fr.linkit.api.connection.packet.persistence.{PacketDeserializationResult, PacketSerializer}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.packet.SimplePacketAttributes
import fr.linkit.engine.connection.packet.fundamental.EmptyPacket
import fr.linkit.engine.connection.packet.persistence.context.SimplePacketConfig

import java.nio.ByteBuffer
import scala.reflect.{ClassTag, classTag}

class LazyPacketDeserializationResult(override val buff: ByteBuffer,
                                      serializer: PacketSerializer) extends PacketDeserializationResult {

    private lazy  val deserial                      = serializer.deserializePacket(buff)(new SimplePacketConfig {})
    private lazy  val cache     : Array[AnyRef]     = createCache()
    override lazy val coords    : PacketCoordinates = deserial.getCoordinates
    override lazy val attributes: PacketAttributes  = extract[PacketAttributes](SimplePacketAttributes.empty)
    override lazy val packet    : Packet            = extract[Packet](EmptyPacket).prepare()

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
        deserial.forEachObjects {
            case attributes: PacketAttributes => cache(0) = attributes
            case packet: Packet               => cache(1) = packet
        }
        AppLogger.debug("Deserialization done.")
        cache
    }

}