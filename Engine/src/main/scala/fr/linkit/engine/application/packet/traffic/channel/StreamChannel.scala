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

package fr.linkit.engine.application.packet.traffic.channel

import java.io.{DataInputStream, DataOutputStream}

import fr.linkit.api.application.packet.channel.ChannelScope
import fr.linkit.api.application.packet.traffic.PacketInjectableStore
import fr.linkit.api.application.packet.{ChannelPacketBundle, Packet}
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.engine.application.packet.UnexpectedPacketException
import org.jetbrains.annotations.Nullable

class StreamChannel(store: PacketInjectableStore, scope: ChannelScope) extends AbstractPacketChannel(store, scope) {

    @Nullable private var input : DataInputStream  = _
    @Nullable private var output: DataOutputStream = _
    @volatile private var transferConstantly       = false

    override def handleBundle(bundle: ChannelPacketBundle): Unit = {
        bundle.packet match {
            case packet: StreamPacket =>
                output.write(packet.streamSlice)
            case other                => throw UnexpectedPacketException(s"Received forbidden packet $other")
        }
    }

    def transferAll(): Unit = {
        val available = input.available()
        val buff      = new Array[Byte](available)
        input.readFully(buff)
        scope.sendToAll(new StreamPacket(buff))
    }

    def startConstantTransfer(): Unit = {
        WorkerPools.ensureCurrentIsNotWorker("This worker thread can't be undefinitely locked.")
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
