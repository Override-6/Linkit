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

package fr.linkit.engine.connection.packet.persistence.context

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.packet.persistence.context._
import fr.linkit.engine.connection.packet.persistence.context.profile.DefaultTypeProfile
import fr.linkit.engine.connection.packet.persistence.context.profile.persistence.{ConstructorTypePersistence, DeconstructiveTypePersistence, SynchronizedObjectsPersistence, UnsafeTypePersistence}
import fr.linkit.engine.local.utils.ClassMap

import scala.collection.mutable

class SimplePersistenceConfig private[context](context: PersistenceContext,
                                               customProfiles: ClassMap[TypeProfile[_]],
                                               referenceStore: WeakReferencedObjectStore,
                                               unsafeUse: Boolean,
                                               referenceAllObjects: Boolean, wide: Boolean) extends PersistenceConfig {

    private val defaultProfiles = mutable.HashMap.empty[Class[_], TypeProfile[_]]

    override def getReferenceStore: MutableReferencedObjectStore = referenceStore

    override def getProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        var profile = defaultProfiles.get(clazz).orNull
        val defaultProfileNull = profile == null
        if (defaultProfileNull) {
            profile = customProfiles.getOrElse(clazz, newDefaultProfile[T](clazz))
        }
        if (isSyncClass(clazz)) {
            @inline
            def cast[X](a: Any) = a.asInstanceOf[X]

            profile = newSynchronizedObjectDefaultProfile(clazz, cast(profile))
        }
        if (defaultProfileNull) {
            defaultProfiles.put(clazz, profile)
        }
        profile.asInstanceOf[TypeProfile[T]]
    }

    override def informObjectReceived(obj: AnyRef): Unit = {
        informObject(obj)
    }

    override def informObjectSent(obj: AnyRef): Unit = {
        informObject(obj)
    }

    @inline
    private def informObject(obj: AnyRef): Unit = {
        if (referenceAllObjects) referenceStore += obj
    }

    private def newSynchronizedObjectDefaultProfile[T <: SynchronizedObject[T]](clazz: Class[_], profile: TypeProfile[T]): DefaultTypeProfile[T] = {
        val network                                 = context.getNetwork
        val persistences: Array[TypePersistence[T]] = profile.getPersistences.map(persist => new SynchronizedObjectsPersistence[T](persist, network))
        new DefaultTypeProfile[T](clazz, this, persistences)
    }

    protected def newDefaultProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        val nonSyncClass                    = if (isSyncClass(clazz)) clazz.getSuperclass else clazz
        val constructor                     = context.findConstructor[T](nonSyncClass)
        val persistence: TypePersistence[T] = {
            if (classOf[Deconstructive].isAssignableFrom(nonSyncClass)) {
                constructor.fold(new DeconstructiveTypePersistence[T with Deconstructive](nonSyncClass)) {
                    ctr => new DeconstructiveTypePersistence[T with Deconstructive](nonSyncClass, ctr.asInstanceOf[java.lang.reflect.Constructor[T with Deconstructive]])
                }
            }.asInstanceOf[TypePersistence[T]] else {
                val deconstructor = context.findDeconstructor[T](nonSyncClass)
                constructor.fold(getSafestTypeProfileOfAnyClass[T](nonSyncClass, deconstructor)) { ctr =>
                    if (deconstructor.isEmpty) {
                        if (!unsafeUse)
                            throw new NoSuchElementException(s"Could not find constructor: A Deconstructor must be set for the class '${nonSyncClass.getName}' in order to create a safe TypePersistence.")
                        new UnsafeTypePersistence[T](nonSyncClass)
                    } else {
                        new ConstructorTypePersistence[T](nonSyncClass, ctr, deconstructor.get)
                    }
                }
            }
        }
        //No (null) parent: No declared profile has been found for the class, and declared profiles automatically handles implementations.
        new DefaultTypeProfile[T](nonSyncClass, null, Array(persistence))
    }

    @inline
    def isSyncClass(clazz: Class[_]): Boolean = {
        classOf[SynchronizedObject[_]].isAssignableFrom(clazz)
    }

    private def getSafestTypeProfileOfAnyClass[T <: AnyRef](clazz: Class[_], deconstructor: Option[Deconstructor[T]]): TypePersistence[T] = {
        val constructor = ConstructorTypePersistence.findConstructor[T](clazz)
        if (constructor.isEmpty || deconstructor.isEmpty) {
            if (!unsafeUse)
                throw new NoSuchElementException(s"Could not find constructor: A Constructor and a Deconstructor must be set for the class '${clazz.getName}' in order to create a safe TypePersistence.")
            return new UnsafeTypePersistence[T](clazz)
        }
        new ConstructorTypePersistence[T](clazz, constructor.get, deconstructor.get)
    }

    override def widePacket: Boolean = wide

    override def useUnsafe: Boolean = unsafeUse
}
