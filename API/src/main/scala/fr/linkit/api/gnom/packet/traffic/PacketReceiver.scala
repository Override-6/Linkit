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

import fr.linkit.api.gnom.packet.channel.PacketChannel
import fr.linkit.api.gnom.packet.{PacketBundle, Packet}

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