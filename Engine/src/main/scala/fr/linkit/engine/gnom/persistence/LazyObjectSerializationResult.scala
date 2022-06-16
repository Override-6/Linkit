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

package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.gnom.persistence.{ObjectPersistence, ObjectSerializationResult, TransferInfo}
import fr.linkit.engine.gnom.persistence.LazyObjectSerializationResult.{BufferLength, buffers}

import java.nio.ByteBuffer
import java.util

abstract class LazyObjectSerializationResult(info: TransferInfo,
                                             private val serializer: ObjectPersistence) extends ObjectSerializationResult {
    
    override val coords: PacketCoordinates = info.coords
    
    override val attributes: PacketAttributes = info.attributes
    
    override val packet: Packet = info.packet
    
    protected def writeCoords(buff: ByteBuffer): Unit
    
    override lazy val buff: ByteBuffer = {
        val buffStack = buffers.get()
        if (buffStack.isEmpty)
            buffStack.push(ByteBuffer.allocate(BufferLength))
        val buff = buffStack.pop()
        buff.clear().position(4)
        //buff.limit(buff.capacity())
        writeCoords(buff)
        info.makeSerial(serializer, buff)
        buff.putInt(0, buff.position() - 4) //write the packet's length
        buffStack.push(buff)
        buff.flip()
    }
    
}

object LazyObjectSerializationResult {
    
    private final val BufferLength = 10_000_000 //10 Mb max per packets
    
    val buffers = ThreadLocal.withInitial(() => new util.Stack[ByteBuffer]())
    
}