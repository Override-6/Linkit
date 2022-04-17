package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.CacheAlreadyDeclaredException
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.EmptyContractDescriptorData
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor

class StaticAccesses(network: Network) {

    private val cacheManager   = network.attachToCacheManager("StaticAccesses")
    private val staticAccesses = cacheManager.attachToCache(-1, DefaultSynchronizedObjectCache[StaticAccess])

    def getStaticAccess(id: Int): StaticAccess = {
        staticAccesses.findObject(id).get
    }

    def newStaticAccess(id: Int, contract: ContractDescriptorData = EmptyContractDescriptorData): StaticAccess = {
        if (id == -1)
            throw new IllegalArgumentException("id can't be -1.") //already took by the staticAccesses cache.
        newStaticAccess0(id, contract)
    }

    protected def newStaticAccess0(id: Int, contract: ContractDescriptorData): StaticAccess = {
        staticAccesses.findObject(id) match {
            case Some(_) =>
                throw new CacheAlreadyDeclaredException(s"Static access already exists for id '$id'")
            case None    =>
                val cache = cacheManager.attachToCache(id, DefaultSynchronizedStaticsCache.apply(contract))
                staticAccesses.syncObject(id, Constructor[StaticAccessImpl](cache), contract)
        }
    }
}
