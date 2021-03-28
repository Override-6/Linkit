/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.connection.network

import fr.linkit.api.connection.packet.{Packet, PacketCoordinates}
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.connection.packet.traffic.channel.SyncAsyncPacketChannel
import fr.linkit.core.local.utils.ConsumerContainer

class RemoteFragmentController(val nameIdentifier: String, val channel: SyncAsyncPacketChannel) {

    private val listeners = ConsumerContainer[(Packet, PacketCoordinates)]()

    channel.addAsyncListener((packet, coords) => {
        packet match {
            case WrappedPacket(this.nameIdentifier, subPacket) => listeners.applyAll((subPacket, coords))
            case _ =>
        }
    })

    def addOnRequestReceived(callback: (Packet, PacketCoordinates) => Unit): Unit = {
        listeners += (tuple2 => callback(tuple2._1, tuple2._2))
    }

    def sendRequest(packet: Packet): Unit = {
        channel.sendRequest(WrappedPacket(nameIdentifier, packet))
    }

    def sendResponse(packet: Packet): Unit = {
        channel.sendResponse(packet)
    }

    def nextResponse[P <: Packet]: P = channel.nextResponse.asInstanceOf[P]

    override def toString: String = s"RemoteFragmentController($nameIdentifier)"

}
