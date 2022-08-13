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

package fr.linkit.client.connection

import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.ClientConnectionConfiguration
import fr.linkit.client.connection.traffic.ClientPacketTraffic
import fr.linkit.engine.gnom.packet.traffic.{DefaultPacketReader, DynamicSocket}
import fr.linkit.engine.internal.concurrency.PacketReaderThread
import fr.linkit.engine.internal.system.SystemPacketChannel

case class ClientConnectionSession(socket: DynamicSocket,
                                   info: ClientConnectionSessionInfo) {
    
    val appContext       : ClientApplication             = info.appContext
    val configuration    : ClientConnectionConfiguration = info.configuration
    val currentIdentifier: String                        = configuration.identifier
    val translator       : ObjectTranslator              = info.translator
    val serverIdentifier : String                        = info.serverIdentifier
    val traffic          : ClientPacketTraffic           = new ClientPacketTraffic(socket, translator, configuration.defaultPersistenceConfigScript, appContext, currentIdentifier, serverIdentifier)
    val packetReader     : DefaultPacketReader           = new DefaultPacketReader(socket, appContext, traffic, translator)
    val readThread       : PacketReaderThread            = new PacketReaderThread(packetReader, serverIdentifier)
    val systemChannel    : SystemPacketChannel           = null // FIXME traffic.getInjectable(SystemChannelID, SystemPacketChannel, ChannelScopes.discardCurrent)
    
}
