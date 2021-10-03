/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.application.plugin.fragment

import fr.linkit.api.gnom.packet.traffic.PacketSender
import fr.linkit.api.application.plugin.fragment.RemoteFragment
import fr.linkit.engine.gnom.packet.traffic.channel.AsyncPacketChannel
import org.jetbrains.annotations.NotNull

abstract class LinkitRemoteFragment extends LinkitPluginFragment with RemoteFragment {

    private var channel: AsyncPacketChannel = _

    val nameIdentifier: String

    @NotNull
    protected def packetSender(): PacketSender = {
        if (channel == null)
            throw new IllegalStateException("Attempted to send a packet from a non initialised remote fragment")
        channel
    }

    private[fragment] def setChannel(channel: AsyncPacketChannel): Unit = {
        if (this.channel != null)
            throw new IllegalStateException("Attempted to override the currently used packet channel.")

        this.channel = channel
        channel.addOnPacketReceived(handleRequest)
    }

}
