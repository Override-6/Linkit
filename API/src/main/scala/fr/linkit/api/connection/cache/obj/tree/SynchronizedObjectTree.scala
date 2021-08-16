package fr.linkit.api.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.{SynchronizedObject, SynchronizedObjectCenter}

trait SynchronizedObjectTree[A <: AnyRef] {

    val center: SynchronizedObjectCenter[A]

    val id: Int

    def rootNode: SyncNode[A]

    val behaviorTree: SynchronizedObjectBehaviorStore

    def findNode[B <: AnyRef](path: Array[Int]): Option[SyncNode[B]]

    def insertObject[B <: AnyRef](parent: SyncNode[_], id: Int, obj: B, ownerID: String): SyncNode[B]

    def insertObject[B <: AnyRef](parentPath: Array[Int], id: Int, obj: B, ownerID: String): SyncNode[B]

}
