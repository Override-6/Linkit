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
import fr.linkit.api.gnom.network._
import fr.linkit.api.internal.system.Versions
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor
import fr.linkit.engine.internal.language.bhv.Contract
import fr.linkit.engine.internal.mapping.RemoteClassMappings
import fr.linkit.engine.internal.system.StaticVersions

import java.sql.Timestamp

class DefaultEngine(override val identifier: String,
                    override val cache: SharedCacheManager) extends Engine {

    override val reference: EngineReference = new EngineReference(identifier)
    override val network  : Network         = cache.network

    //val reference: EngineReference = new EngineReference(identifier)
    override val versions: Versions = StaticVersions.currentVersions

    val classMappings: RemoteClassMappings = {
        val contracts = Contract("NetworkContract")(network)
        cache.attachToCache(1, DefaultSynchronizedObjectCache[RemoteClassMappings](contracts))
                .syncObject(0, Constructor[RemoteClassMappings](identifier))
    }

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    private var connectionState: ExternalConnectionState = ExternalConnectionState.CONNECTED

    def updateState(state: ExternalConnectionState): Unit = connectionState = state

    override def getConnectionState: ExternalConnectionState = connectionState

}
