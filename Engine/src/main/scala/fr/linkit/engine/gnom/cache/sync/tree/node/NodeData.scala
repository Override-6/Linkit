package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.SyncObjectReference
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.tree.DefaultSynchronizedObjectTree

class NodeData[A <: AnyRef](val reference: SyncObjectReference, //The sync object reference
                            val presence: NetworkObjectPresence, //the sync object presence
                            val tree: DefaultSynchronizedObjectTree[_], //The object's tree
                            val currentIdentifier: String, //the current identifier
                            val ownerID: String, //the owner Identifier
                            val parent: Option[MutableSyncNode[_]]) { // the parent of the node

    def this(other: NodeData[A]) = {
        this(other.reference, other.presence, other.tree, other.currentIdentifier, other.ownerID, other.parent)
    }

}