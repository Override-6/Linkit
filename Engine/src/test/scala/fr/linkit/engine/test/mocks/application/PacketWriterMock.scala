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

package fr.linkit.engine.test.mocks.application

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}
import fr.linkit.api.gnom.packet.traffic.{PacketTraffic, PacketWriter}
import fr.linkit.engine.test.mocks.application.LinkitApplicationMock.ServerIdentifier

class PacketWriterMock(override val path: Array[Int],
                       override val traffic: PacketTraffic) extends PacketWriter {
    
    override val serverIdentifier : String        = ServerIdentifier
    override val currentIdentifier: String        = traffic.currentIdentifier
    
    override def writePacket(packet: Packet, targetIDs: Array[String]): Unit = ???
    
    override def writePacket(packet: Packet, attributes: PacketAttributes, targetIDs: Array[String]): Unit = ???
    
    override def writeBroadcastPacket(packet: Packet, discardedIDs: Array[String]): Unit = ???
    
    override def writeBroadcastPacket(packet: Packet, attributes: PacketAttributes, discardedIDs: Array[String]): Unit = ???
}
