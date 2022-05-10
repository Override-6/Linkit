package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.EngineTags._
import fr.linkit.api.gnom.cache.sync.contract.behavior.{ConnectedObjectContext, EngineTag, IdentifierTag}
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer

case class UsageConnectedObjectContext(ownerID: String, rootOwnerID: String,
                                       currentID: String, cacheOwnerID: String,
                                       classDef: SyncClassDef,
                                       choreographer: InvocationChoreographer) extends ConnectedObjectContext {

    override def translate(tag: EngineTag): String = tag match {
        case IdentifierTag(identifier) => identifier
        case CurrentEngine             => currentID
        case OwnerEngine               => ownerID
        case RootOwnerEngine           => rootOwnerID
        case CacheOwnerEngine          => cacheOwnerID
    }
}
