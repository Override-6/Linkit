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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.tree.ConnectedObjectNode
import fr.linkit.api.gnom.network.IdentifierTag
import fr.linkit.api.gnom.packet.channel.request.Submitter
import fr.linkit.engine.gnom.cache.sync.invokation.remote.InvocationPacket
import fr.linkit.engine.gnom.packet.traffic.channel.request.ResponseSubmitter

trait TrafficInterestedNode[A <: AnyRef] extends ConnectedObjectNode[A] {

    def handlePacket(packet: InvocationPacket, senderID: IdentifierTag, response: Submitter[Unit]): Unit

}
