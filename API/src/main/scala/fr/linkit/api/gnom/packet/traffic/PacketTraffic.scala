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

package fr.linkit.api.gnom.packet.traffic

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes, PacketBundle}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.gnom.referencing.linker.NetworkObjectLinker
import fr.linkit.api.internal.system.JustifiedCloseable

trait PacketTraffic extends JustifiedCloseable with PacketInjectableStore {
    
    val currentIdentifier: String
    
    val serverIdentifier: String
    
    def application: ApplicationContext
    
    def connection: ConnectionContext
    
    def getTrafficObjectLinker: NetworkObjectLinker[TrafficReference]
    
    @inline
    def processInjection(packet: Packet, attr: PacketAttributes, coordinates: DedicatedPacketCoordinates): Unit
    
    def processInjection(bundle: PacketBundle): Unit
    
    def processInjection(result: PacketDownload): Unit
    
    def newWriter(path: Array[Int]): PacketWriter
    
    def newWriter(path: Array[Int], persistenceConfig: PersistenceConfig): PacketWriter
    
    def getPersistenceConfig(path: Array[Int]): PersistenceConfig
    
    def findNode(path: Array[Int]): Option[TrafficNode[PacketInjectable]]


}

object PacketTraffic {
    
    val SystemChannelID = 1
    val RemoteConsoles  = 2
}