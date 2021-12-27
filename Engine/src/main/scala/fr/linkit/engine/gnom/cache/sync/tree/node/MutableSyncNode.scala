package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.tree.{ObjectSyncNode, SyncNode}

trait MutableSyncNode[A <: AnyRef] extends SyncNode[A] {
    def discoverParent(node: ObjectSyncNodeImpl[_]): Unit

    def addChild(child: MutableSyncNode[_]): Unit

    def getMatchingSyncNode(nonSyncObject: AnyRef): ObjectSyncNode[_ <: AnyRef]

}
