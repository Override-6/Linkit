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

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.gnom.persistence.{PacketDownload, PacketUpload}
import fr.linkit.engine.gnom.packet.traffic.TrafficProtocol

import java.nio.ByteBuffer

class DeflectedPacket(override val buff: ByteBuffer,
                      override val coords: PacketCoordinates) extends PacketDownload with PacketUpload {
    
    private val trafficBuff = ByteBuffer.allocate(buff.capacity() + 10).put(10, buff.array())
    
    
    override val ordinal: Int = -1 //is not used
    
    override def buff(ordinal: () => Int): ByteBuffer = {
        trafficBuff.putShort(0, TrafficProtocol.ProtocolVersion)
        trafficBuff.putInt(TrafficProtocol.OrdinalIndex, ordinal())
        trafficBuff.putInt(TrafficProtocol.PacketLengthIndex, buff.capacity()) //write the packet content length
    }
    
    override def makeDeserialization(): Unit = throw new UnsupportedOperationException
    
    override def isDeserialized: Boolean = false
    
    override def isInjected: Boolean = false
    
    override def informInjected: Unit = throw new UnsupportedOperationException
    
    override def attributes: PacketAttributes = throw new UnsupportedOperationException
    
    override def packet: Packet = throw new UnsupportedOperationException
}
