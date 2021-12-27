package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.tree.{SyncNode, SyncObjectReference, SynchronizedObjectTree}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence

class NodeData[A <: AnyRef](val reference: SyncObjectReference, //The sync object reference
                            val presence: NetworkObjectPresence, //the sync object presence
                            val tree: SynchronizedObjectTree[_],
                            val currentIdentifier: String,
                            val ownerID: String,
                            val parent: Option[MutableSyncNode[_]]) {


}
