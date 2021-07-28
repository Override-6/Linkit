package fr.linkit.api.connection.cache.repo.tree

import fr.linkit.api.connection.cache.repo.{Chip, Puppeteer}
import org.jetbrains.annotations.Nullable

import scala.collection.mutable.ListBuffer

trait SyncNode[A] {

    lazy val treeViewPath: Array[Int] = {
        var parent: SyncNode[_] = this
        val buff                = ListBuffer.empty[Int]
        while (parent != null) {
            buff.insert(0, parent.id)
            parent = parent.parent
        }
        buff.toArray
    }
    val puppeteer: Puppeteer[A]

    val chip: Chip[A]

    val id: Int

    @Nullable val parent: SyncNode[_]

    def isPresentOnEngine(engineID: String): Boolean

    def putPresence(engineID: String): Unit

    def getChild[B](id: Int): Option[SyncNode[B]]

    def addChild(child: SyncNode[_]): Unit

    def getGrandChild(relativePath: Array[Int]): Option[SyncNode[_]] = {
        var child: SyncNode[_] = this
        for (childID <- relativePath) {
            val opt = child.getChild(childID)
            if (opt.isEmpty)
                return None
            child = opt.get
        }
        Option(child)
    }
}
