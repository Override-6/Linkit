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
import fr.linkit.api.gnom.cache.sync.contract.annotation.LinkedBehavior
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.network._
import fr.linkit.api.gnom.network.statics.{ClassStaticAccessor, StaticAccess}
import fr.linkit.api.internal.system.Versions
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.network.statics.SimpleStaticAccess
import fr.linkit.engine.internal.system.StaticVersions

import java.sql.Timestamp

@LinkedBehavior("contracts/NetworkContract.bhv")
class DefaultEngine(override val identifier: String,
                    override val cache: SharedCacheManager) extends Engine {

    override val reference   : EngineReference = new EngineReference(identifier)
    override val network     : Network         = cache.network
    override val staticAccess: StaticAccess    = {
        val center = cache.attachToCache(5, DefaultSynchronizedObjectCache[ClassStaticAccessor[_ <: AnyRef]](null: ContractDescriptorData))
        new SimpleStaticAccess(center)
    }

    //val reference: EngineReference = new EngineReference(identifier)
    override val versions: Versions = StaticVersions.currentVersions

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    private var connectionState: ExternalConnectionState = ExternalConnectionState.CONNECTED

    def updateState(state: ExternalConnectionState): Unit = connectionState = state

    override def getConnectionState: ExternalConnectionState = connectionState

}
