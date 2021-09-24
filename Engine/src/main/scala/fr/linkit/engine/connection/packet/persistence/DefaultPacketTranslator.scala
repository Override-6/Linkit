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

import java.nio.ByteBuffer

import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.persistence._
import fr.linkit.api.connection.packet.traffic.PacketTraffic
import fr.linkit.api.local.ApplicationContext
import fr.linkit.engine.connection.cache.obj.generation.{DefaultSyncClassCenter, SyncObjectClassResource}
import fr.linkit.engine.connection.packet.persistence.DefaultPacketTranslator.ClassesResourceDirectory
import fr.linkit.engine.connection.packet.persistence.serializor.DefaultPacketSerializer
import fr.linkit.engine.local.LinkitApplication

class DefaultPacketTranslator(app: ApplicationContext) extends PacketTranslator {

    private val serializer = {
        import fr.linkit.engine.local.resource.external.LocalResourceFolder._
        val resources      = app.getAppResources.getOrOpenThenRepresent[SyncObjectClassResource](ClassesResourceDirectory)
        val compilerCenter = app.compilerCenter
        val center         = new DefaultSyncClassCenter(compilerCenter, resources)
        new DefaultPacketSerializer(center)
    }

    override def translate(packetInfo: TransferInfo): PacketSerializationResult = {
        new LazyPacketSerializationResult(packetInfo, serializer)
    }

    override def translate(traffic: PacketTraffic, buff: ByteBuffer): PacketDeserializationResult = {
        val deserial = serializer.deserializePacket(buff)
        val path     = deserial.getCoordinates.path
        val config   = traffic.getPersistenceConfig(path)
        new LazyPacketDeserializationResult(buff, deserial, config)
    }

    override def getSerializer: PacketSerializer = serializer

    override val signature: Array[Byte] = serializer.signature

}

object DefaultPacketTranslator {

    private val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir.classes")
}
