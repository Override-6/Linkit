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

package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.gnom.persistence.{ObjectPersistence, PacketUpload, TransferInfo}
import fr.linkit.engine.gnom.packet.traffic.TrafficProtocol
import fr.linkit.engine.gnom.persistence.PacketUploadImpl.{BufferLength, HoleLength, buffers}

import java.nio.ByteBuffer
import java.util

abstract class PacketUploadImpl(info: TransferInfo,
                                private val serializer: ObjectPersistence) extends PacketUpload {
    
    override val coords: PacketCoordinates = info.coords
    
    override val attributes: PacketAttributes = info.attributes
    
    override val packet: Packet = info.packet
    
    protected def writeCoords(buff: ByteBuffer): Unit
    
    private lazy val buffer: ByteBuffer = {
        val buffStack = buffers.get()
        if (buffStack.isEmpty)
            buffStack.push(ByteBuffer.allocate(BufferLength))
        val buff = buffStack.pop()
        buff.clear()
        buff.putShort(TrafficProtocol.ProtocolVersion)
        buff.position(buff.position() + HoleLength)
        
        //buff.limit(buff.capacity())
        writeCoords(buff)
        info.makeSerial(serializer, buff)
        buff.putInt(TrafficProtocol.PacketLengthIndex, buff.position() - HoleLength - 2) //write the packet's length
        buffStack.push(buff)
        buff.flip()
    }
    
    override def buff(ordinal: () => Int): ByteBuffer = {
        buffer.putInt(TrafficProtocol.OrdinalIndex, ordinal())
    }
}

object PacketUploadImpl {
    
    private final val BufferLength = 10_000_000 //10 Mb max per packets
    
    val buffers = ThreadLocal.withInitial(() => new util.Stack[ByteBuffer]())
    
    //let a hole for 2 ints: packet length and packet ordinal.
    private final val HoleLength = 8
    
}