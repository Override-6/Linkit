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

package fr.linkit.engine.gnom.packet.traffic.channel

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.engine.gnom.packet.traffic.ObjectManagementChannelReference
import fr.linkit.engine.gnom.packet.traffic.channel.request.SimpleRequestPacketChannel
import fr.linkit.engine.gnom.referencing.presence.SystemNetworkObjectPresence

class SystemObjectManagementChannel(scope: ChannelScope)
    extends SimpleRequestPacketChannel(scope) with ObjectManagementChannel {
    override lazy val presence = SystemNetworkObjectPresence

    override def makeReference = ObjectManagementChannelReference
}