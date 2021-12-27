package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.gnom.cache.sync.{SynchronizedObject, SynchronizedObjectCache}
import fr.linkit.engine.gnom.cache.sync.tree.node.SyncNodeDataFactory

trait InternalSynchronizedObjectCache[A <: AnyRef] extends SynchronizedObjectCache[A] with SyncNodeDataFactory {
    def makeTree(root: SynchronizedObject[A]): Unit
}
