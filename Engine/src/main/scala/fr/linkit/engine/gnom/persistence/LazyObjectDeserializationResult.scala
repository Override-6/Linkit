/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.persistence

import java.nio.ByteBuffer

import fr.linkit.api.gnom.persistence.ObjectDeserializationResult
import fr.linkit.api.gnom.persistence.ObjectPersistence.PacketDeserial
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket

import scala.reflect.{ClassTag, classTag}

class LazyObjectDeserializationResult(override val buff: ByteBuffer,
                                      deserial: PacketDeserial,
                                      config: PersistenceConfig) extends ObjectDeserializationResult {

    override val coords: PacketCoordinates = deserial.getCoordinates

    private lazy  val cache     : Array[AnyRef]    = createCache()
    override lazy val attributes: PacketAttributes = extract[PacketAttributes](SimplePacketAttributes.empty)
    override lazy val packet    : Packet           = extract[Packet](EmptyPacket).prepare()

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
        deserial.forEachObjects(config) {
            case attributes: PacketAttributes => cache(0) = attributes
            case packet: Packet               => cache(1) = packet
        }
        AppLogger.debug("Deserialization done.")
        cache
    }

}