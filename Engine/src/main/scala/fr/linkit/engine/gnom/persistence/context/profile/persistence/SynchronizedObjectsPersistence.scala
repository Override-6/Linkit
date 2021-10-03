/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.persistence.context.profile.persistence

import fr.linkit.api.gnom.cache.NoSuchCacheException
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.tree.SyncNodeReference
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.persistence.context.TypePersistence
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.api.gnom.reference.MutableReferencedObjectStore
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCenter
import fr.linkit.engine.gnom.persistence.context.structure.SyncObjectStructure

class SynchronizedObjectsPersistence[T <: SynchronizedObject[T]](objectPersistence: TypePersistence[T], network: Network) extends TypePersistence[T] {

    override val structure: ObjectStructure = new SyncObjectStructure(objectPersistence.structure)

    override def initInstance(syncObj: T, args: Array[Any]): Unit = {
        objectPersistence.initInstance(syncObj, args)
        val info = args.last.asInstanceOf[SyncNodeReference]
        val path = info.nodePath
        val center  = findCache(info)
                .getOrElse {
                    throwNoSuchCacheException(info, Some(syncObj.getSuperClass))
                }
        val treeOpt = center.treeCenter.findTreeInternal(path.head)
        if (treeOpt.isEmpty) {
            AppLogger.error(s"No Object Tree found of id ${path.head}")
            return
        }
        val tree    = treeOpt.get
        val nodeOpt = tree.findNode(path)
        if (nodeOpt.isEmpty) {
            tree.registerSynchronizedObject(path.dropRight(1), path.last, syncObj, info.owner).synchronizedObject
        } else if (nodeOpt.get.synchronizedObject ne syncObj) {
            AppLogger.error(s"Synchronized object already exists at path ${path.mkString("/")}")
        }
    }

    override def toArray(t: T): Array[Any] = {
        (objectPersistence.toArray(t): Array[Any]) :+ (t.getLocation: Any)
    }

    private def findCache(info: SyncNodeReference): Option[DefaultSynchronizedObjectCenter[AnyRef]] = {
        val family = info.cacheFamily
        network.findCacheManager(family)
                .map(_.getCacheInStore[DefaultSynchronizedObjectCenter[AnyRef]](info.cacheID))
    }

    private def throwNoSuchCacheException(info: SyncNodeReference, wrappedClass: Option[Class[_]]): Nothing = {
        throw new NoSuchCacheException(s"Could not find object tree of id ${info.nodePath.head} in synchronized object cache id ${info.cacheID} from cache manager ${info.cacheFamily} " +
                s": could not properly deserialize and synchronize Wrapper object of class \"${wrappedClass.map(_.getName).getOrElse("(Unknown Wrapped class)")}\".")
    }
}
