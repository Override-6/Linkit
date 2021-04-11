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

import fr.linkit.api.connection.packet.serialization.{Serializer, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.fundamental.EmptyPacket

import scala.collection.mutable.ListBuffer

case class PartialTransferInfo(coordsTuple: (PacketCoordinates, Array[Byte]),
                               attributesTuple: (PacketAttributes, Array[Byte]),
                               packetTuple: (Packet, Array[Byte])) extends TransferInfo {

    override val coords    : PacketCoordinates = coordsTuple._1
    override val attributes: PacketAttributes  = attributesTuple._1
    override val packet    : Packet            = packetTuple._1

    val coordsBytes    : Option[Array[Byte]] = Option(coordsTuple._2)
    val attributesBytes: Option[Array[Byte]] = Option(attributesTuple._2)
    val packetBytes    : Option[Array[Byte]] = Option(packetTuple._2)

    override def makeSerial(serializer: Serializer): Array[Byte] = {


        val toSerialize = ListBuffer.empty[Serializable]
        val serialized  = ListBuffer.empty[Array[Byte]]

        def alternate(bytes: Option[Array[Byte]], alternative: Serializable): Unit = {
            if (bytes.isDefined) {
                serialized += bytes.get
            } else {
                toSerialize += alternative
            }
        }

        alternate(coordsBytes, coords)
        if (attributes.nonEmpty)
            alternate(attributesBytes, attributes)

        if (packet != EmptyPacket)
            alternate(packetBytes, packet)

        serializer.partialSerialize(serialized.toArray, toSerialize.toArray)
    }

}
