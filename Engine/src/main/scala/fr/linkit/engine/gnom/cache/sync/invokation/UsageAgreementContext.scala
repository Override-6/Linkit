package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.AgreementContext

case class UsageAgreementContext(ownerID: String, rootOwnerID: String,
                                 currentID: String, cacheOwnerID: String) extends AgreementContext {
    val currentIsCacheOwner: Boolean = ownerID == cacheOwnerID
    val currentIsOwner     : Boolean = ownerID == currentID
    val currentIsRootOwner : Boolean = rootOwnerID == currentID
}
