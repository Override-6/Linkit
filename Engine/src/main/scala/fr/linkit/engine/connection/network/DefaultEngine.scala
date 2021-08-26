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
import fr.linkit.api.connection.cache.obj.behavior.annotation.{BasicInvocationRule, MethodControl}
import fr.linkit.api.connection.network.{Engine, ExternalConnectionState, Network, StaticAccessor}
import fr.linkit.api.local.system.Versions
import fr.linkit.engine.local.system.StaticVersions

import java.sql.Timestamp

class DefaultEngine(override val identifier: String,
                    @transient override val cache: SharedCacheManager) extends Engine {

    @transient override val network       : Network        = cache.network
    override            val staticAccessor: StaticAccessor = null

    override val versions: Versions = StaticVersions.currentVersions

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    private var connectionState: ExternalConnectionState = ExternalConnectionState.CONNECTED

    override def getConnectionState: ExternalConnectionState = connectionState

    @MethodControl(BasicInvocationRule.BROADCAST_IF_ROOT_OWNER)
    def updateState(state: ExternalConnectionState): Unit = connectionState = state

    override def update(): this.type = {
        cache.update()
        this
    }
}
