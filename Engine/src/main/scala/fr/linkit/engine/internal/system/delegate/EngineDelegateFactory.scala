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

package fr.linkit.engine.internal.system.delegate

import fr.linkit.api.gnom.cache.sync.ConnectedObjectCacheFactories
import fr.linkit.api.internal.concurrency.pool.WorkerPools
import fr.linkit.api.internal.system.delegate.DelegateFactoryDelegate
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.internal.concurrency.pool.EngineWorkerPools

class EngineDelegateFactory extends DelegateFactoryDelegate {
    override val workerPools: WorkerPools.Provider = EngineWorkerPools

    override def defaultCOCFactories: ConnectedObjectCacheFactories = DefaultConnectedObjectCache
}
