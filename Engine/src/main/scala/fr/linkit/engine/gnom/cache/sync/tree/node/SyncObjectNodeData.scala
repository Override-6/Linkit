package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer

class SyncObjectNodeData[A <: AnyRef](val puppeteer: Puppeteer[A],
                                      val synchronizedObject: A with SynchronizedObject[A])
                                     (data: ChippedObjectNodeData[A])
        extends ChippedObjectNodeData[A](data) {

    def this(other: SyncObjectNodeData[A]) = {
        this(other.puppeteer, other.synchronizedObject)(other.data)
    }

}