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

package fr.linkit.engine.connection.packet.traffic.channel

import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.engine.connection.packet.UnexpectedPacketException
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import org.jetbrains.annotations.Nullable

import java.io.{DataInputStream, DataOutputStream}

class StreamChannel(parent: PacketChannel, scope: ChannelScope) extends AbstractPacketChannel(parent, scope) {

    @Nullable private var input : DataInputStream  = _
    @Nullable private var output: DataOutputStream = _
    @volatile private var transferConstantly       = false

    override def handleInjection(injection: PacketInjection): Unit = {
        injection.attachPin { (packet, _) =>
            packet match {
                case packet: StreamPacket =>
                    output.write(packet.streamSlice)
                case p                    => throw UnexpectedPacketException(s"Received forbidden packet $p")
            }
        }

        def transferAll(): Unit = {
            val available = input.available()
            val buff      = new Array[Byte](available)
            input.readFully(buff)
            scope.sendToAll(new StreamPacket(buff))
        }

        def startConstantTransfer(): Unit = {
            BusyWorkerPool.ensureCurrentIsNotWorker("This worker thread can't be undefinitely locked.")
            transferConstantly = true
            while (transferConstantly) {
                transferAll()
            }
        }

        def stopConstantTransfer(): Unit = transferConstantly = false

        def inputStream: DataInputStream = input

        def outputStream: DataOutputStream = output

        def setInput(input: DataInputStream): Unit = this.input = input

        def setOutput(output: DataOutputStream): Unit = this.output = output

        class StreamPacket(val streamSlice: Array[Byte]) extends Packet

    }
}
