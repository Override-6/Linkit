package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer

import java.lang.ref.WeakReference

class SyncObjectNodeData[A <: AnyRef](val puppeteer: Puppeteer[A],
                                      synchronizedObject: A with SynchronizedObject[A],
                                      val syncLevel: SyncLevel,
                                      val origin: Option[WeakReference[A]])
                                     (private val data: ChippedObjectNodeData[A])
        extends ChippedObjectNodeData[A](data) {

    def this(other: SyncObjectNodeData[A]) = {
        this(other.puppeteer, other.obj, other.syncLevel, other.origin)(other.data)
    }

    override def obj: A with SynchronizedObject[A] = synchronizedObject

}