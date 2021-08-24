package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.behavior.{ObjectBehaviorStore, ObjectBehavior}
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo

trait ObjectWrapperInstantiator {

    def newWrapper[A <: AnyRef](obj: A, store: ObjectBehaviorStore, puppeteerInfo: SyncNodeInfo, subWrappers: Map[AnyRef, SyncNodeInfo]): (A with SynchronizedObject[A], Map[AnyRef, SynchronizedObject[AnyRef]])

    def initializeWrapper[B <: AnyRef](wrapper: SynchronizedObject[B], nodeInfo: SyncNodeInfo, store: ObjectBehaviorStore): Unit

}
