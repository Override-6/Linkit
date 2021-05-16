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

package fr.linkit.engine.connection.network

import fr.linkit.api.connection.cache.SharedCacheManager
import fr.linkit.api.connection.network.{Engine, ExternalConnectionState, StaticAccessor}
import fr.linkit.api.local.system.Versions

import java.sql.Timestamp

abstract class AbstractRemoteEngine(override val identifier: String,
                                    override val cache: SharedCacheManager) extends Engine {

    override def connectionDate: Timestamp = cache.getInstanceOrWait[Timestamp](2)

    override def versions: Versions = cache.getInstanceOrWait[Versions](3)

    override def update(): this.type = {
        cache.update()
        this
    }

    override def getConnectionState: ExternalConnectionState

    override def toString: String = s"${getClass.getSimpleName}(identifier: $identifier, state: $getConnectionState)"

    override val staticAccessor: StaticAccessor = null
}
