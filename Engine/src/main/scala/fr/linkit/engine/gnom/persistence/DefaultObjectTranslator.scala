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

import java.nio.ByteBuffer

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates}
import fr.linkit.api.gnom.persistence._
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.reference.linker.GeneralNetworkObjectLinker
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.gnom.cache.sync.generation.sync.{DefaultSyncClassCenter, SyncObjectClassResource}
import fr.linkit.engine.gnom.persistence.DefaultObjectTranslator.{BroadcastedFlag, DedicatedFlag}
import fr.linkit.engine.gnom.persistence.serializor.DefaultObjectPersistence

import scala.annotation.switch

class DefaultObjectTranslator(app: ApplicationContext) extends ObjectTranslator {

    private val serializer = {
        import fr.linkit.engine.application.resource.external.LocalResourceFolder._
        val prop           = LinkitApplication.getProperty("compilation.working_dir.classes")
        val resources      = app.getAppResources.getOrOpenThenRepresent[SyncObjectClassResource](prop)
        val compilerCenter = app.compilerCenter
        val center         = new DefaultSyncClassCenter(compilerCenter, resources)
        new DefaultObjectPersistence(center)
    }

    override def translate(packetInfo: TransferInfo): ObjectSerializationResult = {
        new LazyObjectSerializationResult(packetInfo, serializer) {
            override def writeCoords(buff: ByteBuffer): Unit = coords match {
                //TODO Better broadcast persistence handling
                // <------------------------ MAINTAINED ------------------------>
                case BroadcastPacketCoordinates(path, senderID, discardTargets, targetIDs) =>
                    throw new UnsupportedOperationException("Can't send broadcast packets.")
                /*buff.put(BroadcastedFlag) //set the broadcast flag
                buff.putInt(path.length) //path length
                path.foreach(buff.putInt) //path content
                putString(senderID, buff) //senderID String
                buff.put((if (discardTargets) 1 else 0): Byte) //discardTargets boolean
                buff.putInt(targetIDs.length) //targetIds length
                targetIDs.foreach(putString(_, buff)) //targetIds content
                */
                case DedicatedPacketCoordinates(path, targetID, senderID) =>
                    buff.put(DedicatedFlag) //set the dedicated flag
                    buff.putInt(path.length) //path length
                    path.foreach(buff.putInt) //path content
                    putString(targetID, buff) // targetID String
                    putString(senderID, buff) // senderID String
                case _                                                    =>
                    throw new UnsupportedOperationException(s"Coordinates of type '${coords.getClass.getName}' are not supported.")
            }
        }
    }

    private def readCoordinates(buff: ByteBuffer): DedicatedPacketCoordinates = {
        (buff.get(): @switch) match {
            //TODO Better broadcast persistence handling
            // <------------------------ MAINTAINED ------------------------>
            case BroadcastedFlag =>
                throw new UnsupportedOperationException("Can't read broadcast packet.")
            /*
            val path = new Array[Int](buff.getInt) //init path array
            for (i <- path.indices) path(i) = buff.getInt() //fill path content
            val senderId       = getString(buff) //senderID string
            val discardTargets = buff.get == 1 //discardTargets boolean
            val targetIds      = new Array[String](buff.getInt) //init targetIds array
            for (i <- targetIds.indices) targetIds(i) = getString(buff) //fill path content
            BroadcastPacketCoordinates(path, senderId, discardTargets, targetIds)*/
            case DedicatedFlag =>
                val path = new Array[Int](buff.getInt) //init path array
                for (i <- path.indices) path(i) = buff.getInt() //fill path array
                val targetID = getString(buff) //targetID string
                val senderID = getString(buff) //senderID string
                DedicatedPacketCoordinates(path, targetID, senderID)
            case unknown       => throw new MalFormedPacketException(s"Unknown packet coordinates flag '$unknown'.")
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

    override def translate(traffic: PacketTraffic, buffer: ByteBuffer): ObjectDeserializationResult = {
        val coords  = readCoordinates(buffer)
        val conf    = traffic.getPersistenceConfig(coords.path)
        val network = traffic.connection.network
        val bundle  = new PersistenceBundle {
            override val buff        : ByteBuffer                 = buffer
            override val boundId     : String                     = coords.senderID
            override val packetPath  : Array[Int]                 = coords.path
            override val config      : PersistenceConfig          = conf
            override val gnol        : GeneralNetworkObjectLinker = network.gnol
        }
        new LazyObjectDeserializationResult(buffer, coords)(serializer.deserializeObjects(bundle))
    }

}

object DefaultObjectTranslator {

    private final val DedicatedFlag  : Byte = -20
    private final val BroadcastedFlag: Byte = -21
}
