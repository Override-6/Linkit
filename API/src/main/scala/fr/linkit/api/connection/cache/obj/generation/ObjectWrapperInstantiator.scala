package fr.linkit.api.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.api.connection.cache.obj.description.{ObjectTreeBehavior, PuppeteerInfo}

trait ObjectWrapperInstantiator {

    def newWrapper[A](obj: A, behaviorTree: ObjectTreeBehavior, puppeteerInfo: PuppeteerInfo): A with PuppetWrapper[A]

}
