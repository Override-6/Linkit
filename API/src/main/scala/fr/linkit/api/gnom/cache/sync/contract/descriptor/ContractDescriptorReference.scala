package fr.linkit.api.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.SharedCacheReference
import fr.linkit.api.gnom.reference.NetworkObjectReference

class ContractDescriptorReference(val name: String) extends NetworkObjectReference {
    
    override def asSuper: Option[SharedCacheReference] = None
    
    override def toString: String = s"@network/contracts/$name"
}
