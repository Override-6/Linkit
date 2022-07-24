package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.gnom.cache.sync.{SynchronizedObject, ConnectedObjectCache}
import fr.linkit.engine.gnom.cache.sync.tree.node.NodeDataFactory

trait InternalConnectedObjectCache[A <: AnyRef] extends ConnectedObjectCache[A] with NodeDataFactory {
    def requestTree(id: Int): Unit
}
