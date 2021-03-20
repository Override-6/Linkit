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

package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.network.{ConnectionState, NetworkEntity}

import java.sql.Timestamp

abstract class AbstractRemoteEntity(override val identifier: String,
                                    override val cache: SharedCacheManager) extends NetworkEntity {

    override def connectionDate: Timestamp = cache.getOrWait(2)

    override def update(): this.type = {
        cache.update()
        this
    }

    override def getConnectionState: ConnectionState

    override def toString: String = s"${getClass.getSimpleName}(identifier: $identifier, state: $getConnectionState)"
}
