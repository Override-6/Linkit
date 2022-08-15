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

package fr.linkit.api.internal.system.delegate

import fr.linkit.api.gnom.cache.sync.ConnectedObjectCacheFactories
import fr.linkit.api.gnom.cache.sync.contract.Contract
import fr.linkit.api.internal.concurrency.pool.WorkerPools

import java.util.ServiceLoader

object DelegateFactory {
    private val delegate: DelegateFactoryDelegate = {
        val it = ServiceLoader.load(classOf[DelegateFactoryDelegate]).iterator()
        if (it.hasNext) {
            val delegate = it.next()
            if (it.hasNext)
                throw new IllegalStateException("Found more than one DelegateFactoryDelegate service.")
            delegate
        } else {
            throw new IllegalStateException("Could not find any DelegateFactoryDelegate service.")
        }
    }

    def workerPools: WorkerPools.Provider = delegate.workerPools

    def defaultCOCFactories: ConnectedObjectCacheFactories = delegate.defaultCOCFactories

    def contracts: Contract.Provider = delegate.contracts
}
