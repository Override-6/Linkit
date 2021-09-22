package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.behavior.ObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.{SyncInstanceGetter, SynchronizedObject}

trait ObjectWrapperInstantiator {

    def newWrapper[A <: AnyRef](creator: SyncInstanceGetter[A], store: ObjectBehaviorStore, puppeteerInfo: SyncNodeInfo, subWrappers: Map[AnyRef, SyncNodeInfo]): (A with SynchronizedObject[A], Map[AnyRef, SynchronizedObject[AnyRef]])

    def initializeSyncObject[B <: AnyRef](wrapper: SynchronizedObject[B], nodeInfo: SyncNodeInfo, store: ObjectBehaviorStore): Unit

}
