package fr.linkit.api.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.SharedCacheReference

class ContractDescriptorReference(family: String, cacheID: Int, val identifier: Int) extends SharedCacheReference(family, cacheID) {

    override def asSuper: Option[SharedCacheReference] = Some(new SharedCacheReference(family, cacheID))

    override def toString: String = s"@network/caches/$family/$cacheID/contracts/$identifier"
}
