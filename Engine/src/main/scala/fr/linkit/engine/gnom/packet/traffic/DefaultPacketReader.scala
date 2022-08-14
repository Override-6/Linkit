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

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.packet.traffic.{PacketReader, PacketTraffic}
import fr.linkit.api.gnom.persistence.{ObjectTranslator, PacketDownload}
import fr.linkit.api.internal.concurrency.{ProcrastinatorControl, workerExecution}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.util.NumberSerializer
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
    override def nextPacket(@workerExecution callback: PacketDownload => Unit): Unit = {
        nextPacketSync(result => procrastinator.runLater {
            AppLoggers.Traffic.trace(s"Thread is handling packet injection with ordinal number ${result.ordinal}")
            callback(result)
        })
    }
    
    private def nextPacketSync(callback: PacketDownload => Unit): Unit = {
        val protocol = socket.readShort()
        if (protocol != TrafficProtocol.ProtocolVersion) {
            AppLoggers.Traffic.error(s"Received packet packet with unknown protocol '$protocol', closing connection with ${socket.boundIdentifier}...")
            socket.close() //TODO close the connection instead of its socket.
            throw new UnsupportedPacketStreamException(s"Received packet packet with unknown protocol '$protocol'")
        }
        
        val ordinal = socket.readInt()
        val length = socket.readInt()
        if (length == -1 || socket.isClosed) {
            AppLoggers.Traffic.error(s"PACKET READ ABORTED : packet length: $length | socket.isOpen = ${socket.isOpen}")
            return
        }
        val bytes = socket.read(length)
        //NETWORK-DEBUG-MARK
        
        logDownload(socket.boundIdentifier, ordinal, length, bytes)
        val buff = ByteBuffer.wrap(bytes)
        //buff.position(0)
        val result = translator.translate(traffic, buff, ordinal)
        callback(result)
    }
    
    private def logDownload(@Nullable target: String, ordinal: Int, length: Int, bytes: Array[Byte]): Unit = if (AppLoggers.Traffic.isTraceEnabled) {
        var preview = new String(bytes.take(1000)).replace('\n', ' ').replace('\r', ' ')
        if (bytes.length > 1000) preview += "..."
        val finalTarget = if (target == null) "" else target
        AppLoggers.Traffic.trace(s"${Console.CYAN}Received: ↓ $finalTarget ↓ (len: $length, ord: $ordinal) $preview")
    }
    
}
