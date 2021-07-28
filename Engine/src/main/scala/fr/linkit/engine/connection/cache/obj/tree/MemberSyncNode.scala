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

package fr.linkit.engine.connection.cache.obj.tree

import fr.linkit.api.connection.cache.repo.tree.SyncNode
import fr.linkit.engine.connection.cache.obj.invokation.remote.InvocationPacket
import fr.linkit.engine.connection.packet.traffic.channel.request.ResponseSubmitter

trait MemberSyncNode[A] extends SyncNode[A]{

    def handlePacket(packet: InvocationPacket, response: ResponseSubmitter): Unit

}
