/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.network

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.{BasicInvocationRule, MethodControl}
import fr.linkit.api.gnom.network._
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.internal.system.Versions
import fr.linkit.engine.internal.system.StaticVersions

import java.sql.Timestamp

class DefaultEngine(override val identifier: String,
                    override val cache: SharedCacheManager) extends Engine {

    override val network     : Network      = cache.network
    override val staticAccess: StaticAccess = null

    //val reference: EngineReference = new EngineReference(identifier)
    override val versions: Versions = StaticVersions.currentVersions

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    private var connectionState: ExternalConnectionState = ExternalConnectionState.CONNECTED

    @MethodControl(BasicInvocationRule.BROADCAST_IF_ROOT_OWNER) //Root owner is the Network object owner, which is the server.
    def updateState(state: ExternalConnectionState): Unit = connectionState = state

    override def getConnectionState: ExternalConnectionState = connectionState

    override def update(): this.type = {
        cache.update()
        this
    }
}
