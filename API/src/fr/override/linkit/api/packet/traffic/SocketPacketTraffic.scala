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

package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.packet.Packet

class SocketPacketTraffic(@NotNull relay: Relay,
                          @NotNull socket: DynamicSocket,
                          @NotNull override val ownerID: String) extends AbstractPacketTraffic(relay.configuration, relay.identifier) {

    private val translator = relay.packetTranslator

    override def newWriter(identifier: Int, transform: Packet => Packet): PacketWriter = {
        new SocketPacketWriter(socket, translator, WriterInfo(this, identifier, transform))
    }

}
