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

package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.packet.serialization.{PacketTransferResult, Serializer}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.EmptyPacket

import scala.reflect.{ClassTag, classTag}

case class LazyPacketDeserializationResult(override val bytes: Array[Byte],
                                           serializer: () => Serializer) extends PacketTransferResult {

    private lazy  val cache                         = serializer().deserializeAll(bytes)
    override lazy val coords    : PacketCoordinates = extract[PacketCoordinates](null)
    override lazy val attributes: PacketAttributes  = extract[PacketAttributes](SimplePacketAttributes.empty)
    override lazy val packet    : Packet            = extract[Packet](EmptyPacket).prepare()

    private def extract[T <: Serializable : ClassTag](orElse: => T): T = {
        val clazz       = classTag[T].runtimeClass
        val coordsIndex = cache.indexWhere(o => clazz.isAssignableFrom(o.getClass))

        if (coordsIndex < 0) {
            val alternative = orElse
            if (alternative == null)
                throw MalFormedPacketException(bytes, s"Received unknown packet array (${cache.mkString("Array(", ", ", ")")})")
            else return alternative
        }
        cache(coordsIndex) match {
            case e: T => e
        }
    }

}