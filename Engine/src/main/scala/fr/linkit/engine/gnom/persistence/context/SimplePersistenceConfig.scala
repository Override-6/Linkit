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

package fr.linkit.engine.gnom.persistence.context

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.persistence.context._
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.api.gnom.reference.linker.ContextObjectLinker
import fr.linkit.api.gnom.reference.traffic.TrafficInterestedNPH
import fr.linkit.engine.gnom.persistence.context.profile.DefaultTypeProfile
import fr.linkit.engine.gnom.persistence.context.profile.persistence.{ConstructorTypePersistence, DeconstructiveTypePersistence, SynchronizedObjectsPersistence, UnsafeTypePersistence}
import fr.linkit.engine.gnom.persistence.context.structure.ArrayObjectStructure
import fr.linkit.engine.internal.utils.ClassMap

import scala.collection.mutable

class SimplePersistenceConfig private[linkit](context: PersistenceContext,
                                              customProfiles: ClassMap[TypeProfile[_]],
                                              override val contextualObjectLinker: ContextObjectLinker with TrafficInterestedNPH,
                                              override val autoContextObjects: Boolean,
                                              override val useUnsafe: Boolean,
                                              override val widePacket: Boolean) extends PersistenceConfig {

    private val cachedProfiles = mutable.HashMap.empty[Class[_], TypeProfile[_]]

    override def getProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        var profile = cachedProfiles.get(clazz).orNull
        if (profile eq null) {
            val isSync = isSyncClass(clazz)
            profile = customProfiles.get(clazz) match {
                case Some(value: TypeProfile[T]) if isSync => warpTypePersistencesWithSyncPersist[T](value)
                case Some(value)                           => value
                case None                                  => newDefaultProfile(clazz)
            }
            cachedProfiles.put(clazz, profile)
        }
        profile.asInstanceOf[TypeProfile[T]]
    }

    private def newDefaultProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        val isSync                          = isSyncClass(clazz)
        val nonSyncClass                    = if (isSync) clazz.getSuperclass else clazz
        val constructor                     = context.findConstructor[T](nonSyncClass)
        var persistence: TypePersistence[T] = {
            if (classOf[Deconstructible].isAssignableFrom(nonSyncClass)) {
                constructor.fold(new DeconstructiveTypePersistence[T with Deconstructible](nonSyncClass)) {
                    ctr => new DeconstructiveTypePersistence[T with Deconstructible](nonSyncClass, ctr.asInstanceOf[java.lang.reflect.Constructor[T with Deconstructible]])
                }
            }.asInstanceOf[TypePersistence[T]] else {
                val deconstructor = context.findDeconstructor[T](nonSyncClass)
                constructor.fold(getSafestTypeProfileOfAnyClass[T](nonSyncClass, deconstructor)) { ctr =>
                    if (deconstructor.isEmpty) {
                        if (!useUnsafe)
                            throw new NoSuchElementException(s"Could not find constructor: A Deconstructor must be set for the class '${nonSyncClass.getName}' in order to create a safe TypePersistence.")
                        new UnsafeTypePersistence[T](nonSyncClass)
                    } else {
                        new ConstructorTypePersistence[T](nonSyncClass, ctr, deconstructor.get)
                    }
                }
            }
        }
        if (isSync) {
            persistence = new SynchronizedObjectsPersistence[Nothing](persistence).asInstanceOf[TypePersistence[T]]
        }
        new DefaultTypeProfile[T](nonSyncClass, this, Array(persistence))
    }

    @inline
    private def isSyncClass(clazz: Class[_]): Boolean = {
        classOf[SynchronizedObject[_]].isAssignableFrom(clazz)
    }

    private def getSafestTypeProfileOfAnyClass[T <: AnyRef](clazz: Class[_], deconstructor: Option[Deconstructor[T]]): TypePersistence[T] = {
        val constructor = ConstructorTypePersistence.findPersistConstructor[T](clazz)
        if (constructor.isEmpty || deconstructor.isEmpty) {
            //FIXME if (!useUnsafe)
            //    throw new NoSuchElementException(s"Could not find constructor: A Constructor and a Deconstructor must be set for the '${clazz}' in order to create a safe TypePersistence.")
            return new UnsafeTypePersistence[T](clazz)
        }
        new ConstructorTypePersistence[T](clazz, constructor.get, deconstructor.get)
    }

    private def warpTypePersistencesWithSyncPersist[T <: AnyRef](profile: TypeProfile[T]): TypeProfile[T] = {
        val persistences = profile.getPersistences.map(new SynchronizedObjectsPersistence[Nothing](_).asInstanceOf[TypePersistence[T]])
        new DefaultTypeProfile[T](profile.typeClass, this, persistences)
    }

}

object SimplePersistenceConfig {

    //Used for Remote Synchronized Objects
    private final val FullRemoteObjectPersistence = new TypePersistence[AnyRef] {
        override val structure: ObjectStructure = ArrayObjectStructure()

        override def initInstance(allocatedObject: AnyRef, args: Array[Any], box: ControlBox): Unit = ()

        override def toArray(t: AnyRef): Array[Any] = Array.empty
    }
}
