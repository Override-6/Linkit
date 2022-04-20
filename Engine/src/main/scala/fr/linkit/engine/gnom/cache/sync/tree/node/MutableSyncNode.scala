package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.tree.ObjectNode

trait MutableSyncNode[A <: AnyRef] extends ObjectNode[A] {
    def discoverParent(node: ObjectSyncNodeImpl[_]): Unit

    def addChild(child: MutableSyncNode[_]): Unit

    def getMatchingSyncNode(nonSyncObject: AnyRef): MutableSyncNode[_ <: AnyRef]

}
