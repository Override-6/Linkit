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

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.reference.{NetworkObjectLinker, NetworkObjectReference}
import fr.linkit.engine.gnom.persistence.PacketSerializationChoreographer

import java.net.URL

class SocketPacketTraffic(socket: DynamicSocket,
                          translator: ObjectTranslator,
                          defaultPersistenceConfigScript: Option[URL],
                          override val application: ApplicationContext,
                          override val currentIdentifier: String,
                          override val serverIdentifier: String) extends AbstractPacketTraffic(currentIdentifier, defaultPersistenceConfigScript) {

    private lazy val choreographer                                       = new PacketSerializationChoreographer(translator)
    private var connection0: ConnectionContext                           = _
    private var gnol       : NetworkObjectLinker[NetworkObjectReference] = _

    override def connection: ConnectionContext = connection0

    def setConnection(connection: ConnectionContext): Unit = {
        if (connection0 != null)
            throw new IllegalStateException("Connection already set !")
        this.connection0 = connection
    }

    def setGnol(gnol: NetworkObjectLinker[NetworkObjectReference]): Unit = {
        if (this.gnol != null)
            throw new IllegalStateException("gnol already set !")
        this.gnol = gnol
    }

    override def newWriter(path: Array[Int], persistenceConfig: PersistenceConfig): PacketWriter = {
        if (persistenceConfig == null)
            throw new NullPointerException("persistenceConfig is null.")
        new SocketPacketWriter(socket, choreographer, WriterInfo(this, persistenceConfig, path, () => gnol))
    }

}
