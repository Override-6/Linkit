package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.EngineTags._
import fr.linkit.api.gnom.cache.sync.contract.behavior.{AgreementContext, EngineTag, IdentifierTag}

case class UsageAgreementContext(ownerID: String, rootOwnerID: String,
                                 currentID: String, cacheOwnerID: String) extends AgreementContext {

    override def translate(tag: EngineTag): String = tag match {
        case IdentifierTag(identifier) => identifier
        case CurrentEngine             => currentID
        case OwnerEngine               => ownerID
        case RootOwnerEngine           => rootOwnerID
        case CacheOwnerEngine          => cacheOwnerID
    }
}
