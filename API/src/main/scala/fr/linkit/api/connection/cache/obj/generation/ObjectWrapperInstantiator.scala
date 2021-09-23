package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.behavior.ObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.instantiation.SyncInstanceGetter

trait ObjectWrapperInstantiator {

    def newWrapper[A <: AnyRef](creator: SyncInstanceGetter[A], store: ObjectBehaviorStore, puppeteerInfo: SyncNodeInfo): A with SynchronizedObject[A]

    def initializeSyncObject[B <: AnyRef](wrapper: SynchronizedObject[B], nodeInfo: SyncNodeInfo, store: ObjectBehaviorStore): Unit

}
