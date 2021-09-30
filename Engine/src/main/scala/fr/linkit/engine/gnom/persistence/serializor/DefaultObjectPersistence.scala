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

package fr.linkit.engine.gnom.persistence.serializor

import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, PacketCoordinates}
import fr.linkit.api.gnom.persistence.obj.PoolObject
import fr.linkit.api.gnom.persistence.{ObjectPersistence, PersistenceBundle}
import fr.linkit.engine.gnom.persistence.MalFormedPacketException
import fr.linkit.engine.gnom.persistence.serializor.DefaultObjectPersistence.{BroadcastedFlag, DedicatedFlag}
import fr.linkit.engine.gnom.persistence.serializor.read.{NotInstantiatedObject, PacketReader}
import fr.linkit.engine.gnom.persistence.serializor.write.{PacketWriter, SerializerObjectPool}

import java.nio.ByteBuffer

class DefaultObjectPersistence(center: SyncClassCenter) extends ObjectPersistence {

    override val signature: Seq[Byte] = Seq(12)

    override def isSameSignature(buffer: ByteBuffer): Boolean = {
        val pos    = buffer.position()
        val result = signature.forall(buffer.get.equals)
        buffer.position(pos)
        result
    }

    override def serializeObjects(objects: Array[AnyRef])(bundle: PersistenceBundle): Unit = {
        val buffer = bundle.buff
        buffer.put(signature.toArray)
        val writer = new PacketWriter(bundle)
        writer.addObjects(objects)
        writer.writePool()
        val pool = writer.getPool
        writeEntries(objects, writer, pool)
    }

    private def writeEntries(objects: Array[AnyRef], writer: PacketWriter,
                             pool: SerializerObjectPool): Unit = {
        //Write the size
        writer.putRef(objects.length)
        //Write the content

        for (o <- objects) {
            val idx = pool.globalPosition(o)
            writer.putRef(idx)
        }
    }

    override def deserializeObjects(bundle: PersistenceBundle)(forEachObjects: AnyRef => Unit): Unit = {
        val buff = bundle.buff
        checkSignature(buff)

        val reader = new PacketReader(bundle, center)
        reader.initPool()
        val contentSize = buff.getChar
        val pool        = reader.getPool
        for (_ <- 0 until contentSize) {
            val obj = pool.getAny(reader.readNextRef) match {
                case o: PoolObject[AnyRef]            => o.value
                case o: AnyRef                        => o
            }
            forEachObjects(obj)
        }
    }

    private def writeCoords(buff: ByteBuffer, coords: PacketCoordinates): Unit = coords match {
        case BroadcastPacketCoordinates(path, senderID, discardTargets, targetIDs) =>
            buff.put(BroadcastedFlag) //set the broadcast flag
            buff.putInt(path.length) //path length
            path.foreach(buff.putInt) //path content
            putString(senderID, buff) //senderID String
            buff.put((if (discardTargets) 1 else 0): Byte) //discardTargets boolean
            buff.putInt(targetIDs.length) //targetIds length
            targetIDs.foreach(putString(_, buff)) //targetIds content

        case DedicatedPacketCoordinates(path, targetID, senderID) =>
            buff.put(DedicatedFlag) //set the dedicated flag
            buff.putInt(path.length) //path length
            path.foreach(buff.putInt) //path content
            putString(targetID, buff) // targetID String
            putString(senderID, buff) // senderID String
        case _                                                    => throw new UnsupportedOperationException(s"Coordinates of type '${coords.getClass.getName}' are not supported.")
    }

    private def readCoordinates(buff: ByteBuffer): PacketCoordinates = {
        buff.get() match {
            case BroadcastedFlag =>
                val path = new Array[Int](buff.getInt) //init path array
                for (i <- path.indices) path(i) = buff.getInt() //fill path content
                val senderId       = getString(buff) //senderID string
                val discardTargets = buff.get == 1 //discardTargets boolean
                val targetIds      = new Array[String](buff.getInt) //init targetIds array
                for (i <- targetIds.indices) targetIds(i) = getString(buff) //fill path content
                BroadcastPacketCoordinates(path, senderId, discardTargets, targetIds)
            case DedicatedFlag   =>
                val path = new Array[Int](buff.getInt) //init path array
                for (i <- path.indices) path(i) = buff.getInt() //fill path content
                val targetID = getString(buff) //targetID string
                val senderID = getString(buff) //senderID string
                DedicatedPacketCoordinates(path, targetID, senderID)
            case unknown         => throw new MalFormedPacketException(s"Unknown packet coordinates flag '$unknown'")
        }
    }

    private def putString(str: String, buff: ByteBuffer): Unit = {
        buff.putInt(str.length).put(str.getBytes())
    }

    private def getString(buff: ByteBuffer): String = {
        val size  = buff.getInt()
        val array = new Array[Byte](size)
        buff.get(array)
        new String(array)
    }

    private def checkSignature(buff: ByteBuffer): Unit = {
        if (!isSameSignature(buff))
            throw new IllegalArgumentException("Signature mismatches !")
        buff.position(buff.position() + signature.length)
    }

}

object DefaultObjectPersistence {

    private val DedicatedFlag  : Byte = -20
    private val BroadcastedFlag: Byte = -21
}
