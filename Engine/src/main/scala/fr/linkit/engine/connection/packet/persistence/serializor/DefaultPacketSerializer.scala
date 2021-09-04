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

package fr.linkit.engine.connection.packet.persistence.serializor

import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperClassCenter
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.persistence.PacketSerializer
import fr.linkit.api.connection.packet.persistence.PacketSerializer.PacketDeserial
import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext}
import fr.linkit.api.connection.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, PacketCoordinates}
import fr.linkit.engine.connection.packet.persistence.serializor.DefaultPacketSerializer.{BroadcastedFlag, DedicatedFlag}

import java.nio.ByteBuffer

class DefaultPacketSerializer(center: ObjectWrapperClassCenter, context: PersistenceContext) extends PacketSerializer {

    override val signature: Array[Byte] = Array(12)

    override def isSameSignature(buffer: ByteBuffer): Boolean = ???

    override def serializePacket(objects: Array[AnyRef], coordinates: PacketCoordinates, buffer: ByteBuffer)(config: PacketConfig): Unit = {
        val writer = new ObjectPoolWriter(config, context, buffer)
        if (config.putSignature)
            buffer.put(signature)
        writeCoords(buffer, coordinates)
        writer.writeObjects(objects)
        writer.writeHeaderSize()
        val pool = writer.getPool
        for (o <- objects) {
            buffer.putChar(pool.indexOf(o).toChar)
        }
    }

    private def writeCoords(buff: ByteBuffer, coords: PacketCoordinates): Unit = coords match {
        case BroadcastPacketCoordinates(path, senderID, discardTargets, targetIDs) =>
            buff.put(BroadcastedFlag) //set the broadcast flag
            buff.putInt(path.length) //path length
            path.foreach(buff.putInt) //path content
            buff.putInt(senderID.length).put(senderID.getBytes()) // senderID String
            buff.put((if (discardTargets) 1 else 0): Byte) //discardTargets boolean
            buff.putInt(targetIDs.length) //targetIds length
            targetIDs.foreach(id => buff.put(id.getBytes()))

        case DedicatedPacketCoordinates(path, targetID, senderID) =>
            buff.put(DedicatedFlag) //set the dedicated flag
            buff.putInt(path.length) //path length
            path.foreach(buff.putInt) //path content
            buff.putInt(targetID.length).put(targetID.getBytes()) // targetID String
            buff.putInt(senderID.length).put(senderID.getBytes()) // senderID String
        case _                                                    =>
    }

    override def deserializePacket(buff: ByteBuffer): PacketSerializer.PacketDeserial = {
        new PacketDeserial {
            override def getCoordinates: PacketCoordinates = ???

            override def forEachObjects(f: Any => Unit): Unit = ???
        }
    }

    def initNetwork(network: Network): Unit = {

    }

}

object DefaultPacketSerializer {

    private val DedicatedFlag  : Byte = 20
    private val BroadcastedFlag: Byte = 21
}
