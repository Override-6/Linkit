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

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.persistence.{ObjectDeserializationResult, ObjectTranslator}
import fr.linkit.api.gnom.packet.traffic.{PacketReader, PacketTraffic}
import fr.linkit.api.internal.concurrency.{ProcrastinatorControl, workerExecution}
import fr.linkit.api.internal.system.AppLoggers
import fr.linkit.api.internal.system.security.BytesHasher
import fr.linkit.engine.internal.utils.NumberSerializer
import org.jetbrains.annotations.Nullable

import java.nio.ByteBuffer

class DefaultPacketReader(socket: DynamicSocket,
                          procrastinator: ProcrastinatorControl,
                          traffic: PacketTraffic,
                          translator: ObjectTranslator) extends PacketReader {
    
    private var packetOrdinal = 0
    
    private def nextOrdinal: Int = {
        packetOrdinal += 1
        packetOrdinal
    }
    
    /**
     * @return a tuple containing the next packet with its coordinates and its local number identifier
     * */
    override def nextPacket(@workerExecution callback: ObjectDeserializationResult => Unit): Unit = {
        nextPacketSync(result => procrastinator.runLater(callback(result)))
    }
    
    def nextPacketSync(callback: ObjectDeserializationResult => Unit): Unit = {
        val nextLength = socket.readInt()
        if (nextLength == -1 || socket.isClosed) {
            AppLoggers.Traffic.error(s"PACKET READ ABORTED : packet length: $nextLength | socket.isOpen = ${socket.isOpen}")
            return
        }
        
        val ordinal = nextOrdinal
        val bytes   = socket.read(nextLength)
        //NETWORK-DEBUG-MARK
        
        logDownload(socket.boundIdentifier, bytes)
        val buff = ByteBuffer.allocate(bytes.length + 4)
        buff.put(NumberSerializer.serializeInt(nextLength))
        buff.put(bytes)
        buff.position(4)
        val result = translator.translate(traffic, buff, ordinal)
        callback(result)
    }
    
    private def logDownload(@Nullable target: String, bytes: Array[Byte]): Unit = if (AppLoggers.Traffic.isInfoEnabled) {
        val preview     = new String(bytes.take(1000)).replace('\n', ' ').replace('\r', ' ')
        val finalTarget = if (target == null) "" else target
        AppLoggers.Traffic.info(s"${Console.CYAN}Received: ↓ $finalTarget ↓ $preview (l: ${bytes.length + 4})")
    }
    
}
