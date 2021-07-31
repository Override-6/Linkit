package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.api.connection.cache.obj.description.{ObjectTreeBehavior, WrapperNodeInfo}

trait ObjectWrapperInstantiator {

    def newWrapper[A](obj: A, behaviorTree: ObjectTreeBehavior, puppeteerInfo: WrapperNodeInfo): A with PuppetWrapper[A]

}
