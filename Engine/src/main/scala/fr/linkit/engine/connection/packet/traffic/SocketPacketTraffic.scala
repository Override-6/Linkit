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

package fr.linkit.engine.connection.packet.traffic

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.connection.packet.persistence.PacketTranslator
import fr.linkit.api.connection.packet.persistence.context.PersistenceConfig
import fr.linkit.api.connection.packet.traffic.PacketWriter
import fr.linkit.api.local.ApplicationContext
import fr.linkit.engine.connection.packet.persistence.PacketSerializationChoreographer

import java.net.URL

class SocketPacketTraffic(socket: DynamicSocket,
                          translator: PacketTranslator,
                          defaultPersistenceConfigScript: Option[URL],
                          override val application: ApplicationContext,
                          override val currentIdentifier: String,
                          override val serverIdentifier: String) extends AbstractPacketTraffic(currentIdentifier, defaultPersistenceConfigScript) {

    private val choreographer                  = new PacketSerializationChoreographer(translator)
    private var connection0: ConnectionContext = _

    override def connection: ConnectionContext = connection0

    def setConnection(connection: ConnectionContext): Unit = {
        if (connection0 != null)
            throw new IllegalStateException("Connection already set !")
        connection0 = connection
    }

    override def newWriter(path: Array[Int], persistenceConfig: PersistenceConfig): PacketWriter = {
        new SocketPacketWriter(socket, choreographer, WriterInfo(this, persistenceConfig, path))
    }

}
