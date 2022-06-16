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

package fr.linkit.client.connection.traffic

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.engine.gnom.packet.traffic.{AbstractPacketTraffic, DynamicSocket, WriterInfo}

import java.net.URL
import scala.collection.mutable

class ClientPacketTraffic(socket: DynamicSocket,
                          translator: ObjectTranslator,
                          defaultPersistenceConfigScript: Option[URL],
                          override val application: ApplicationContext,
                          override val currentIdentifier: String,
                          override val serverIdentifier: String) extends AbstractPacketTraffic(currentIdentifier, defaultPersistenceConfigScript) {
    
    private var connection0: ConnectionContext = _
    private var network    : Network           = _
    
    private final lazy val ordinals = mutable.HashMap.empty[Int, OrdinalCounter]
    
    override def connection: ConnectionContext = connection0
    
    def setConnection(connection: ConnectionContext): Unit = {
        if (connection0 != null)
            throw new IllegalStateException("Connection already set !")
        this.connection0 = connection
    }
    
    def setNetwork(network: Network): Unit = {
        if (this.network != null)
            throw new IllegalStateException("network object already set !")
        this.network = network
    }
    
    override def newWriter(path: Array[Int], persistenceConfig: PersistenceConfig): PacketWriter = {
        if (persistenceConfig == null)
            throw new NullPointerException("persistenceConfig is null.")
        val ordinal = ordinals.getOrElseUpdate(java.util.Arrays.hashCode(path), new OrdinalCounter)
        val info = WriterInfo(this, persistenceConfig, path, () => network)
        new ClientPacketWriter(socket, ordinal, translator, info)
    }
    
}
