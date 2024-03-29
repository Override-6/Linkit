/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket

import java.nio.ByteBuffer
import scala.reflect.{ClassTag, classTag}

class PacketDownloadImpl(override val buff: ByteBuffer,
                         override val coords: PacketCoordinates,
                         override val ordinal: Int)
                        (forEachObjects: (AnyRef => Unit) => Unit) extends PacketDownload {

    private lazy val cache : Array[AnyRef]    = createCache()
    private var attributes0: PacketAttributes = _
    private var packet0    : Packet           = _
    private var injected = false

    override def isInjected: Boolean = injected

    override def informInjected: Unit = injected = true

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

    override def makeDeserialization(): Unit = this.synchronized {
        if (isDeserialized)
            throw new IllegalStateException("Already deserialized !")
        attributes0 = extract[PacketAttributes](SimplePacketAttributes.empty)
        packet0 = extract[Packet](EmptyPacket)
    }

    override def isDeserialized: Boolean = attributes0 != null && packet0 != null

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
        val cache = new Array[AnyRef](2)
        forEachObjects {
            case attributes: PacketAttributes => cache(0) = attributes
            case packet: Packet               => cache(1) = packet
        }
        cache
    }

}