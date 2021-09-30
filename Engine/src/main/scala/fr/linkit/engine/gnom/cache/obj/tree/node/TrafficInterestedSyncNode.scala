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

package fr.linkit.engine.gnom.cache.obj.tree.node

import fr.linkit.api.gnom.cache.sync.tree.SyncNode
import fr.linkit.api.gnom.packet.channel.request.Submitter
import fr.linkit.engine.gnom.cache.obj.invokation.remote.InvocationPacket
import fr.linkit.engine.gnom.packet.traffic.channel.request.ResponseSubmitter

trait TrafficInterestedSyncNode[A <: AnyRef] extends SyncNode[A] {

    def handlePacket(packet: InvocationPacket, senderID: String, response: Submitter[Unit]): Unit

}
