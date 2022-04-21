package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.tree.ConnectedObjectNode
import org.jetbrains.annotations.Nullable

trait MutableNode[A <: AnyRef] extends ConnectedObjectNode[A] {
    def discoverParent(node: ObjectSyncNodeImpl[_]): Unit

    def addChild(child: MutableNode[_]): Unit

}
trait MutableSyncNode[A <: AnyRef] extends MutableNode[A] {
    @Nullable
    def getMatchingSyncNode(origin: AnyRef): MutableSyncNode[_ <: AnyRef]
}