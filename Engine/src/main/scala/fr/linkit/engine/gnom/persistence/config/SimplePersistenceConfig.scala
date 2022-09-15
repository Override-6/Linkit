/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.config

import fr.linkit.api.gnom.cache.sync.{ChippedObject, SynchronizedObject}
import fr.linkit.api.gnom.persistence.context._
import fr.linkit.api.gnom.referencing.linker.ContextObjectLinker
import fr.linkit.api.gnom.referencing.traffic.TrafficInterestedNPH
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter
import fr.linkit.engine.gnom.persistence.config.profile.DefaultTypeProfile
import fr.linkit.engine.gnom.persistence.config.profile.persistence._
import fr.linkit.engine.gnom.persistence.defaults.lambda.{NotSerializableLambdasTypePersistence, SerializableLambdasTypePersistence}
import fr.linkit.engine.gnom.persistence.defaults.special.EmptyObjectTypePersistence
import fr.linkit.engine.internal.util.ClassMap

import scala.collection.mutable

class SimplePersistenceConfig private[linkit](customProfiles: ClassMap[TypeProfile[_]],
                                              override val contextualObjectLinker: ContextObjectLinker with TrafficInterestedNPH) extends PersistenceConfig {
    
    private val cachedProfiles = mutable.HashMap.empty[Class[_], TypeProfile[_]]

    
    private def getLambdaProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        if (classOf[Serializable].isAssignableFrom(clazz))
            new DefaultTypeProfile[T](clazz, this, Array(SerializableLambdasTypePersistence))
        else
            new DefaultTypeProfile[T](clazz, this, Array(NotSerializableLambdasTypePersistence))
    }
    
    override def getProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        if (isLambdaClass(clazz)) {
            return getLambdaProfile(clazz)
        }
        
        var profile = cachedProfiles.get(clazz).orNull
        if (profile eq null) {
            val isSync = isSyncClass(clazz)
            profile = customProfiles.get(clazz) match {
                case Some(profile: TypeProfile[T]) if isSync => warpTypePersistencesWithSyncPersist[T](profile)
                case Some(profile)                           => profile
                case None                                    => newDefaultProfile(clazz)
            }
            cachedProfiles.put(clazz, profile)
        }
        profile.asInstanceOf[TypeProfile[T]]
    }
    
    override def getProfile[T <: AnyRef](ref: T): TypeProfile[T] = {
        val clazz = ref.getClass
        ref match {
            case sync: SynchronizedObject[T] =>
                if (sync.isMirrored) newMirroredObjectProfile[T](clazz)
                else getProfile[T](clazz)
            case _                           =>
                ChippedObjectAdapter.findAdapter(ref) match {
                    case Some(chi) => newMirroredObjectProfile(clazz, chi)
                    case None      => getProfile(clazz)
                }
        }
    }
    
    private def newMirroredObjectProfile[T <: AnyRef](clazz: Class[_], chi: ChippedObject[_] = null): TypeProfile[T] = {
        val syncPersist = if (chi == null) {
            new SynchronizedObjectPersistence[Nothing](EmptyObjectTypePersistence)
        } else {
            new ChippedObjectPersistence(chi, EmptyObjectTypePersistence)
        }.asInstanceOf[TypePersistence[T]]
        new DefaultTypeProfile[T](clazz, this, Array(syncPersist.asInstanceOf[TypePersistence[T]]))
    }
    
    private def getSyncOriginClass(clazz: Class[_]): Class[_] = {
        val sc = clazz.getSuperclass
        if (sc eq classOf[Object]) {
            val interfaces = clazz.getInterfaces
            if (interfaces.length > 1)
                interfaces(1) //the head is AbstractSynchronizedObject
            else sc
        }
        else sc
    }
    
    private def newDefaultProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        val isSync                          = isSyncClass(clazz)
        val nonSyncClass                    = if (isSync) getSyncOriginClass(clazz) else clazz
        var persistence: TypePersistence[T] = determinePersistence(nonSyncClass)
        if (isSync) {
            persistence = new SynchronizedObjectPersistence[Nothing](persistence).asInstanceOf[TypePersistence[T]]
        }
        new DefaultTypeProfile[T](nonSyncClass, this, Array(persistence))
    }
    
    private def determinePersistence[T <: AnyRef](clazz: Class[_]): TypePersistence[T] = {
        if (classOf[Deconstructible].isAssignableFrom(clazz)) {
            new DeconstructiveTypePersistence[T with Deconstructible](clazz).asInstanceOf[TypePersistence[T]]
        } else {
            new RegularTypePersistence[T](clazz)
        }
    }
    
    @inline
    private def isSyncClass(clazz: Class[_]): Boolean = {
        classOf[SynchronizedObject[_]].isAssignableFrom(clazz)
    }
    
    @inline
    private def isLambdaClass(clazz: Class[_]): Boolean = {
        clazz.getSimpleName.contains("$$Lambda$")
    }
    
    private def warpTypePersistencesWithSyncPersist[T <: AnyRef](profile: TypeProfile[T]): TypeProfile[T] = {
        val persistences = {
            (profile.getPersistences :+ EmptyObjectTypePersistence)
                    .map(new SynchronizedObjectPersistence[Nothing](_).asInstanceOf[TypePersistence[T]])
        }
        new DefaultTypeProfile[T](profile.typeClass, this, persistences)
    }
    
}