package fr.linkit.api.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.{PuppetWrapper, SynchronizedObjectCenter}
import fr.linkit.api.connection.cache.obj.description.ObjectTreeBehavior

trait SynchronizedObjectTree[A <: AnyRef] {

    val center: SynchronizedObjectCenter[A]

    val id: Int

    def rootNode: SyncNode[A]

    val behaviorTree: ObjectTreeBehavior

    def findNode[B <: AnyRef](path: Array[Int]): Option[SyncNode[B]]

    def insertObject[B <: AnyRef](parent: SyncNode[_], id: Int, obj: B, ownerID: String): SyncNode[B]

    def insertObject[B <: AnyRef](parentPath: Array[Int], id: Int, obj: B, ownerID: String): SyncNode[B]

}
