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

import fr.linkit.api.connection.packet.channel.PacketChannel
import fr.linkit.api.connection.packet.{PacketBundle, Packet}

import scala.reflect.ClassTag

trait PacketSyncReceiver extends PacketChannel {

    def nextPacket[P <: Packet : ClassTag]: P

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    def haveMorePackets: Boolean
}

trait PacketAsyncReceiver[B <: PacketBundle] extends PacketChannel {

    def addOnPacketReceived(callback: B => Unit): Unit

}