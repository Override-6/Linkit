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

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.network.{ExternalConnectionState, NetworkEntity}
import fr.linkit.api.local.system.AppLogger

import java.sql.Timestamp

abstract class AbstractRemoteEntity(override val identifier: String,
                                    override val entityCache: SharedCacheManager) extends NetworkEntity {

    AppLogger.warn(s"CREATING REMOTE ENTITY ${identifier}")

    override def connectionDate: Timestamp = entityCache.getInstanceOrWait[Timestamp](2)

    override def update(): this.type = {
        entityCache.update()
        this
    }

    override def getConnectionState: ExternalConnectionState

    override def toString: String = s"${getClass.getSimpleName}(identifier: $identifier, state: $getConnectionState)"
}
