package fr.linkit.api.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.invokation.local.Chip
import fr.linkit.api.connection.cache.obj.invokation.remote.Puppeteer
import org.jetbrains.annotations.Nullable

import scala.collection.mutable.ListBuffer

trait SyncNode[A <: AnyRef] {

    lazy val treePath: Array[Int] = {
        var parent: SyncNode[_] = this
        val buff                = ListBuffer.empty[Int]
        while (parent != null) {
            buff += parent.id
            parent = parent.parent
        }
        buff.toArray.reverse
    }

    val tree: SynchronizedObjectTree[_]

    val puppeteer: Puppeteer[A]

    val chip: Chip[A]

    val id: Int

    val ownerID: String

    val synchronizedObject: A with SynchronizedObject[A] = puppeteer.getSynchronizedObject

    @Nullable val parent: SyncNode[_]

    def isPresentOnEngine(engineID: String): Boolean

    def putPresence(engineID: String): Unit

}
