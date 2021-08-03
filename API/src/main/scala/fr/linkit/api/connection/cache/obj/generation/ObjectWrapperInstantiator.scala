package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.api.connection.cache.obj.description.{ObjectTreeBehavior, WrapperBehavior, WrapperNodeInfo}

trait ObjectWrapperInstantiator {

    def newWrapper[A <: AnyRef](obj: A, behaviorTree: ObjectTreeBehavior, puppeteerInfo: WrapperNodeInfo, subWrappers: Map[AnyRef, WrapperNodeInfo]): (A with PuppetWrapper[A], Map[AnyRef, PuppetWrapper[AnyRef]])

    def initializeWrapper[B <: AnyRef](wrapper: PuppetWrapper[B], nodeInfo: WrapperNodeInfo, behavior: WrapperBehavior[B]): Unit

}
