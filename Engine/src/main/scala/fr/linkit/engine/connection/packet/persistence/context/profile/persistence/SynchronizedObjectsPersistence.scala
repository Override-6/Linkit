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

package fr.linkit.engine.connection.packet.persistence.context.profile.persistence

import fr.linkit.api.connection.cache.NoSuchCacheException
import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.tree.NoSuchSyncNodeException
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.persistence.context.TypePersistence
import fr.linkit.api.connection.packet.persistence.obj.ObjectStructure
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter
import fr.linkit.engine.connection.packet.persistence.UnexpectedObjectException
import fr.linkit.engine.connection.packet.persistence.context.structure.SyncObjectStructure

class SynchronizedObjectsPersistence[T <: SynchronizedObject[T]](objectPersistence: TypePersistence[T], network: Network) extends TypePersistence[T] {

    override val structure: ObjectStructure = new SyncObjectStructure(objectPersistence.structure)

    override def initInstance(syncObj: T, args: Array[Any]): Unit = {
        objectPersistence.initInstance(syncObj, args)
        val info   = args.last.asInstanceOf[SyncNodeInfo]
        val path   = info.nodePath
        val center = findCache(info)
                .getOrElse {
                    throwNoSuchCacheException(info, Some(syncObj.getSuperClass))
                }
        val tree   = center.treeCenter.findTreeInternal(path.head).getOrElse {
            throw new NoSuchSyncNodeException(s"No Object Tree found of id ${path.head}") //TODO Replace with NoSuchObjectTreeException
        }

        val nodeOpt = tree.findNode(path)
        if (nodeOpt.isEmpty) {
            tree.registerSynchronizedObject(path.dropRight(1), path.last, syncObj, info.owner).synchronizedObject
        } else if (nodeOpt.get.synchronizedObject ne syncObj) {
            throw new UnexpectedObjectException(s"Synchronized object already exists at path ${path.mkString("/")}")
        }
    }

    override def toArray(t: T): Array[Any] = {
        (objectPersistence.toArray(t): Array[Any]) :+ (t.getNodeInfo: Any)
    }

    private def findCache(info: SyncNodeInfo): Option[DefaultSynchronizedObjectCenter[AnyRef]] = {
        val family = info.cacheFamily
        network.findCacheManager(family)
                .map(_.getCacheInStore[DefaultSynchronizedObjectCenter[AnyRef]](info.cacheID))
    }

    private def throwNoSuchCacheException(info: SyncNodeInfo, wrappedClass: Option[Class[_]]): Nothing = {
        throw new NoSuchCacheException(s"Could not find object tree of id ${info.nodePath.head} in synchronized object cache id ${info.cacheID} from cache manager ${info.cacheFamily} " +
                s": could not properly deserialize and synchronize Wrapper object of class \"${wrappedClass.map(_.getName).getOrElse("(Unknown Wrapped class)")}\".")
    }
}
