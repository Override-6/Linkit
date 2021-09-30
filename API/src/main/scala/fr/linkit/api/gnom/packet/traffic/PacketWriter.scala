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

package fr.linkit.api.gnom.packet.traffic

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}

trait PacketWriter {

    val serverIdentifier : String
    val currentIdentifier: String
    val path             : Array[Int]
    val traffic          : PacketTraffic

    def writePacket(packet: Packet, targetIDs: Array[String]): Unit

    def writePacket(packet: Packet, attributes: PacketAttributes, targetIDs: Array[String]): Unit

    def writeBroadcastPacket(packet: Packet, discardedIDs: Array[String]): Unit

    def writeBroadcastPacket(packet: Packet, attributes: PacketAttributes, discardedIDs: Array[String]): Unit

}
