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

package fr.linkit.server.connection.traffic

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.gnom.persistence.{PacketDownload, PacketUpload}
import fr.linkit.engine.gnom.packet.traffic.TrafficProtocol

import java.nio.ByteBuffer

class DeflectedPacket(override val buff: ByteBuffer,
                      override val coords: PacketCoordinates) extends PacketDownload with PacketUpload {
    
    override val ordinal: Int = -1 //is not used
    
    override def buff(ordinal: Int): ByteBuffer = buff.putInt(TrafficProtocol.OrdinalIndex, ordinal)
    
    override def makeDeserialization(): Unit = throw new UnsupportedOperationException
    
    override def isDeserialized: Boolean = false
    
    override def isInjected: Boolean = false
    
    override def informInjected: Unit = throw new UnsupportedOperationException
    
    override def attributes: PacketAttributes = throw new UnsupportedOperationException
    
    override def packet: Packet = throw new UnsupportedOperationException
}
