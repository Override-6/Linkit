package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.behavior.{ObjectTreeBehavior, WrapperBehavior}
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo

trait ObjectWrapperInstantiator {

    def newWrapper[A <: AnyRef](obj: A, behaviorTree: ObjectTreeBehavior, puppeteerInfo: WrapperNodeInfo, subWrappers: Map[AnyRef, WrapperNodeInfo]): (A with SynchronizedObject[A], Map[AnyRef, SynchronizedObject[AnyRef]])

    def initializeWrapper[B <: AnyRef](wrapper: SynchronizedObject[B], nodeInfo: WrapperNodeInfo, behavior: WrapperBehavior[B]): Unit

}
