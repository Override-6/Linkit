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

package fr.linkit.api.connection.packet.traffic

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.connection.packet.persistence.context.PersistenceConfig
import fr.linkit.api.connection.packet.{DedicatedPacketBundle, DedicatedPacketCoordinates, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.local.ApplicationContext
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.JustifiedCloseable

trait PacketTraffic extends JustifiedCloseable with PacketInjectableStore {

    val currentIdentifier: String

    val serverIdentifier: String

    def application: ApplicationContext

    def connection: ConnectionContext

    @workerExecution
    @inline def processInjection(packet: Packet, attr: PacketAttributes, coordinates: DedicatedPacketCoordinates): Unit

    @workerExecution
    def processInjection(bundle: PacketBundle): Unit

    def newWriter(path: Array[Int]): PacketWriter

    def newWriter(path: Array[Int], persistenceConfig: PersistenceConfig): PacketWriter

    def getPersistenceConfig(path: Array[Int]): PersistenceConfig

}

object PacketTraffic {
    val SystemChannelID = 1
    val RemoteConsoles  = 2
}