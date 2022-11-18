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

package fr.linkit.server.connection.traffic

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.engine.gnom.packet.traffic.{AbstractPacketTraffic, WriterInfo}
import fr.linkit.server.connection.ServerConnection

import java.net.URL

class ServerPacketTraffic(override val connection       : ServerConnection,
                          defaultPersistenceConfigScript: Option[URL],
                          network                       : Network) extends AbstractPacketTraffic(defaultPersistenceConfigScript, network) {

    override def application: ApplicationContext = connection.getApp

    override def newWriter(path: Array[Int], persistenceConfig: PersistenceConfig): PacketWriter = {
        new ServerPacketWriter(connection, WriterInfo(this, persistenceConfig, path, network))
    }
}
