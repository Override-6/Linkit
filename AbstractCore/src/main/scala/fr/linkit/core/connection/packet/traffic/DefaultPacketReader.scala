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

package fr.linkit.core.connection.packet.traffic

import fr.linkit.api.connection.packet.serialization.{PacketTransferResult, PacketTranslator}
import fr.linkit.api.connection.packet.traffic.PacketReader
import fr.linkit.api.local.concurrency.{Procrastinator, workerExecution}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.local.system.security.BytesHasher

class DefaultPacketReader(socket: DynamicSocket,
                          hasher: BytesHasher,
                          procrastinator: Procrastinator,
                          translator: PacketTranslator) extends PacketReader {

    /**
     * @return a tuple containing the next packet with its coordinates and its local number identifier
     * */
    override def nextPacket(@workerExecution callback: PacketTransferResult => Unit): Unit = {
        nextPacketSync(result => procrastinator.runLater(callback(result)))
    }

    def nextPacketSync(callback: PacketTransferResult => Unit): Unit = {
        val nextLength = socket.readInt()
        if (nextLength == -1 || socket.isClosed) {
            AppLogger.error(s"PACKET READ READ WAS ABORTED : $nextLength || ${socket.isOpen}")
            return
        }

        val bytes  = hasher.deHashBytes(socket.read(nextLength))
        //NETWORK-DEBUG-MARK
        AppLogger.logDownload(socket.boundIdentifier, bytes)
        val result = translator.translate(bytes)
        callback(result)
    }
}
