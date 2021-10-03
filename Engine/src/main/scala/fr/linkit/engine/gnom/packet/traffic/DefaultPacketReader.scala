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
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.api.internal.system.security.BytesHasher
import fr.linkit.engine.internal.utils.NumberSerializer
import java.nio.ByteBuffer

class DefaultPacketReader(socket: DynamicSocket,
                          procrastinator: ProcrastinatorControl,
                          traffic: PacketTraffic,
                          translator: ObjectTranslator) extends PacketReader {

    /**
     * @return a tuple containing the next packet with its coordinates and its local number identifier
     * */
    override def nextPacket(@workerExecution callback: ObjectDeserializationResult => Unit): Unit = {
        nextPacketSync(result => procrastinator.runLater(callback(result)))
    }

    def nextPacketSync(callback: ObjectDeserializationResult => Unit): Unit = {
        val nextLength = socket.readInt()
        if (nextLength == -1 || socket.isClosed) {
            AppLogger.error(s"PACKET READ READ WAS ABORTED : $nextLength || ${socket.isOpen}")
            return
        }

        val bytes = socket.read(nextLength)
        //NETWORK-DEBUG-MARK
        AppLogger.logDownload(socket.boundIdentifier, bytes)
        val buff = ByteBuffer.allocate(bytes.length + 4)
        buff.put(NumberSerializer.serializeInt(nextLength))
        buff.put(bytes)
        buff.position(4)
        val result = translator.translate(traffic, buff)
        callback(result)
    }
}
