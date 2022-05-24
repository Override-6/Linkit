/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.config

import fr.linkit.api.gnom.cache.sync.{ChippedObject, SynchronizedObject}
import fr.linkit.api.gnom.persistence.context._
import fr.linkit.api.gnom.reference.linker.ContextObjectLinker
import fr.linkit.api.gnom.reference.traffic.TrafficInterestedNPH
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter
import fr.linkit.engine.gnom.persistence.config.profile.DefaultTypeProfile
import fr.linkit.engine.gnom.persistence.config.profile.persistence._
import fr.linkit.engine.gnom.persistence.defaults.special.EmptyObjectTypePersistence
import fr.linkit.engine.internal.utils.ClassMap

import scala.collection.mutable

class SimplePersistenceConfig private[linkit](context: PersistenceContext,
                                              customProfiles: ClassMap[TypeProfile[_]],
                                              override val contextualObjectLinker: ContextObjectLinker with TrafficInterestedNPH,
                                              override val autoContextObjects: Boolean,
                                              override val useUnsafe: Boolean) extends PersistenceConfig {
    
    private val cachedProfiles = mutable.HashMap.empty[Class[_], TypeProfile[_]]
    
    override def getProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
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
            clazz.getInterfaces()(1) //the head is AbstractSynchronizedObject
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
        val constructor = context.findConstructor[T](clazz)
        if (classOf[Deconstructible].isAssignableFrom(clazz)) {
            constructor.fold(new DeconstructiveTypePersistence[T with Deconstructible](clazz)) { ctr =>
                new DeconstructiveTypePersistence[T with Deconstructible](ctr.asInstanceOf[java.lang.reflect.Constructor[T with Deconstructible]])
            }.asInstanceOf[TypePersistence[T]]
        } else {
            val deconstructor = context.findDeconstructor[T](clazz)
            constructor.fold(getSafestTypeProfileOfAnyClass[T](clazz, deconstructor)) { ctr =>
                if (deconstructor.isEmpty) {
                    if (!useUnsafe)
                        throw new NoSuchElementException(s"Could not find constructor: A Deconstructor must be set for the class '${clazz.getName}' in order to create a safe TypePersistence.")
                    new RegularTypePersistence[T](clazz)
                } else {
                    new ConstructorTypePersistence[T](ctr, deconstructor.get)
                }
            }
        }
    }
    
    @inline
    private def isSyncClass(clazz: Class[_]): Boolean = {
        classOf[SynchronizedObject[_]].isAssignableFrom(clazz)
    }
    
    private def getSafestTypeProfileOfAnyClass[T <: AnyRef](clazz: Class[_], deconstructor: Option[Deconstructor[T]]): TypePersistence[T] = {
        val constructor = if (classOf[Deconstructible].isAssignableFrom(clazz)) ConstructorTypePersistence.findPersistConstructor[T](clazz) else None
        if (constructor.isEmpty || deconstructor.isEmpty) {
            //FIXME if (!useUnsafe)
            //    throw new NoSuchElementException(s"Could not find constructor: A Constructor and a Deconstructor must be set for the '${clazz}' in order to create a safe TypePersistence.")
            return new RegularTypePersistence[T](clazz)
        }
        new ConstructorTypePersistence[T](constructor.get, deconstructor.get)
    }
    
    private def warpTypePersistencesWithSyncPersist[T <: AnyRef](profile: TypeProfile[T]): TypeProfile[T] = {
        val persistences = profile.getPersistences.map(new SynchronizedObjectPersistence[Nothing](_).asInstanceOf[TypePersistence[T]])
        new DefaultTypeProfile[T](profile.typeClass, this, persistences)
    }
    
}