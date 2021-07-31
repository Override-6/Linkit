package fr.linkit.engine.connection.cache.obj

import fr.linkit.api.connection.cache.obj.{PuppetWrapper, SynchronizedObjectCenter}
import fr.linkit.api.connection.cache.obj.description.PuppeteerInfo

trait SynchronizedObjectCenterInternal[A] extends SynchronizedObjectCenter[A] {

    def registerObject(puppeteerInfo: PuppeteerInfo, obj: AnyRef): AnyRef with PuppetWrapper[AnyRef]

}
