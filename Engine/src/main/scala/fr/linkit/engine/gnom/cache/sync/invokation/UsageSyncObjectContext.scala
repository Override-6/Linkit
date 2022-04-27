package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.EngineTags._
import fr.linkit.api.gnom.cache.sync.contract.behavior.{EngineTag, IdentifierTag, SyncObjectContext}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer

case class UsageSyncObjectContext(ownerID: String, rootOwnerID: String,
                                  currentID: String, cacheOwnerID: String,
                                  choreographer: InvocationChoreographer) extends SyncObjectContext {

    override def translate(tag: EngineTag): String = tag match {
        case IdentifierTag(identifier) => identifier
        case CurrentEngine             => currentID
        case OwnerEngine               => ownerID
        case RootOwnerEngine           => rootOwnerID
        case CacheOwnerEngine          => cacheOwnerID
    }
}
