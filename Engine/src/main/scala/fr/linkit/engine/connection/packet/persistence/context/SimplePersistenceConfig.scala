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
        val profile = defaultProfiles.get(clazz)
        if (profile.isEmpty) {
            return customProfiles.getOrElse(clazz, {
                val default: DefaultTypeProfile[T] = newDefaultProfile[T](clazz)
                if (isSyncClass(clazz)) {
                    @inline
                    def cast[X](any: AnyRef): X = any.asInstanceOf[X]

                    val syncDefault = newSynchronizedObjectDefaultProfile[Nothing](clazz, cast(default))
                    defaultProfiles.put(clazz, syncDefault)
                } else {
                    defaultProfiles.put(clazz, default)
                    default
                }
            }).asInstanceOf[TypeProfile[T]]
        }
        profile.get.asInstanceOf[TypeProfile[T]]
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

    private def newSynchronizedObjectDefaultProfile[T <: SynchronizedObject[T]](clazz: Class[_], profile: DefaultTypeProfile[T]): DefaultTypeProfile[T] = {
        val network                                 = context.getNetwork
        val persistences: Array[TypePersistence[T]] = profile.persists.map(persist => new SynchronizedObjectsPersistence[T](persist, network))
        new DefaultTypeProfile[T](clazz, profile, persistences, profile.convertors)
    }

    protected def newDefaultProfile[T <: AnyRef](clazz: Class[_]): DefaultTypeProfile[T] = {
        val constructor                     = context.findConstructor[T](clazz)
        val persistence: TypePersistence[T] = {
            if (classOf[Deconstructive].isAssignableFrom(clazz)) {
                constructor.fold(new DeconstructiveTypePersistence[T with Deconstructive](clazz)) {
                    ctr => new DeconstructiveTypePersistence[T with Deconstructive](clazz, ctr.asInstanceOf[java.lang.reflect.Constructor[T with Deconstructive]])
                }
            }.asInstanceOf[TypePersistence[T]] else {
                val deconstructor = context.findDeconstructor[T](clazz)
                constructor.fold(getSafestTypeProfileOfAnyClass[T](clazz, deconstructor)) { ctr =>
                    if (deconstructor.isEmpty) {
                        if (!unsafeUse)
                            throw new NoSuchElementException(s"Could not find constructor: A Deconstructor must be set for the class '${clazz.getName}' in order to create a safe TypePersistence.")
                        new UnsafeTypePersistence[T](clazz)
                    } else {
                        new ConstructorTypePersistence[T](clazz, ctr, deconstructor.get)
                    }
                }
            }
        }
        //No (null) parent: No declared profile has been found for the class, and declared profiles automatically handles implementations.
        new DefaultTypeProfile[T](clazz, null, Array(persistence), new ClassMap[ObjectConverter[_ <: T, Any]])
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
