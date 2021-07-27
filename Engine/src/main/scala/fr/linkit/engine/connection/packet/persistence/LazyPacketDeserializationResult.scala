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

import fr.linkit.api.connection.packet.persistence.{PacketDeserializationResult, Serializer}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.engine.connection.packet.SimplePacketAttributes
import fr.linkit.engine.connection.packet.fundamental.EmptyPacket

import java.nio.ByteBuffer
import scala.reflect.{ClassTag, classTag}

class LazyPacketDeserializationResult(override val buff: ByteBuffer,
                                      serializer: Serializer) extends PacketDeserializationResult {

    private lazy  val cache     : Array[AnyRef]     = createCache()
    override lazy val coords    : PacketCoordinates = extract[PacketCoordinates](null)
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
        val cache = new Array[AnyRef](3)

        serializer.deserialize(buff) {
            case coords: PacketCoordinates    => cache(0) = coords
            case attributes: PacketAttributes => cache(1) = attributes
            case packet: Packet               => cache(2) = packet
        }

        cache
    }

}