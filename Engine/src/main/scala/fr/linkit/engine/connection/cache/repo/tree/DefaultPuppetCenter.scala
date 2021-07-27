/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.repo.tree

import fr.linkit.api.connection.cache.repo.tree.{PuppetCenter, SyncNode}
import fr.linkit.engine.connection.cache.repo.CacheRepoContent
import fr.linkit.engine.connection.cache.repo.DefaultEngineObjectCenter.PuppetProfile

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DefaultPuppetCenter[A] extends PuppetCenter {

    private val puppets = new mutable.HashMap[Int, SyncNode[A]]

    /**
     * This map stores every children that attempted to attach to a parent that
     * does not yet exists. <br>
     * When children nodes are in this map, they they have no effect on the outside world (no remote method handling etc...).
     * This map is used during packet deserialisation in case of nested synchronised objects
     * The Array[Int] key is the parent's path, and the value is the children's supplier (see [[addNode()]])
     *
     * @see [[addNode()]]
     */
    private val waitingChildren = mutable.HashMap.empty[Array[Int], ListBuffer[(Int, (Int, SyncNode[_]) => SyncNode[_])]]

    override def findNode[B](path: Array[Int]): Option[SyncNode[B]] = puppets.get(path(0)) match {
        case None                           => None
        case Some(value) if path.length > 1 => value
                .getGrandChild(path.drop(1))
                .map(_.asInstanceOf[SyncNode[B]])
        case s                              => s.asInstanceOf[Option[SyncNode[B]]]
    }

    override def addNode[B](path: Array[Int], supplier: (Int, SyncNode[_]) => SyncNode[B]): Unit = {
        if (path.length == 1) {
            val last = puppets.put(path.head, supplier(path.head, null).asInstanceOf[SyncNode[A]])
            if (last.isDefined)
                throw new IllegalStateException(s"Puppet already exists at ${path.mkString("$", " -> ", "")}")
            return
        }

        val parentPath = path.dropRight(1)
        findNode[B](parentPath)
                .fold[Unit] {
                    waitingChildren.getOrElseUpdate(parentPath, ListBuffer.empty) += ((path.last, supplier))
                } {
                    parent => {
                        val child = supplier(path.last, parent)
                        parent.addChild(child)
                        waitingChildren.get(path).fold[Unit]()(_.foreach(childrenOfChild => {
                            val supplier = childrenOfChild._2
                            val id = childrenOfChild._1
                            child.addChild(supplier(id, child))
                            waitingChildren -= path
                        }))
                    }
                }
    }

    override def snapshotContent: CacheRepoContent[A] = {
        def toProfile(node: SyncNode[_ <: A]): PuppetProfile[A] = {
            val puppeteer = node.puppeteer
            PuppetProfile[A](node.treeViewPath, puppeteer.getPuppetWrapper.detachedSnapshot(), puppeteer.puppeteerInfo.owner)
        }

        val array = puppets.values.map(toProfile).toArray
        new CacheRepoContent[A](array)
    }

}
