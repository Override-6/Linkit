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

package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.CacheAlreadyDeclaredException
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.instantiation.New
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.EmptyContractDescriptorData

class StaticAccesses(network: Network) {

    private val cacheManager   = network.findCacheManager("StaticAccesses").getOrElse {
        throw new NoSuchElementException("Could not find cache manager for static accesses")
    }
    //static accesses cache
    private val staCache = cacheManager.attachToCache(-1, DefaultConnectedObjectCache[StaticAccess])

    def getStaticAccess(id: Int): StaticAccess = {
        staCache.findObject(id).get
    }

    def newStaticAccess(id: Int, contract: ContractDescriptorData = EmptyContractDescriptorData): StaticAccess = {
        if (id == -1)
            throw new IllegalArgumentException("id can't be -1.") //already took by the staticAccesses cache.
        newStaticAccess0(id, contract)
    }

    protected def newStaticAccess0(id: Int, contract: ContractDescriptorData): StaticAccess = {
        staCache.findObject(id) match {
            case Some(_) =>
                throw new CacheAlreadyDeclaredException(s"Static access already exists for id '$id'")
            case None    =>
                staCache.syncObject(id, New[StaticAccessImpl](id, cacheManager, contract), contract)
        }
    }
}
