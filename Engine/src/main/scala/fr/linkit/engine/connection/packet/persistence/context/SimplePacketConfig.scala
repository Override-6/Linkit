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

import fr.linkit.api.connection.packet.persistence.context._
import fr.linkit.engine.connection.packet.persistence.context.profile.SimpleTypeProfile
import fr.linkit.engine.connection.packet.persistence.context.profile.persistence.{ConstructorTypePersistence, DeconstructiveTypePersistence, UnsafeTypePersistence}
import fr.linkit.engine.local.utils.ClassMap

import scala.collection.mutable

class SimplePacketConfig private[context](context: PersistenceContext,
                                          customProfiles: ClassMap[TypeProfile[_]],
                                          referenceStore: WeakReferencedObjectStore,
                                          unsafeUse: Boolean, withSignature: Boolean,
                                          referenceAllObjects: Boolean, wide: Boolean) extends PacketConfig {

    private val defaultProfiles = mutable.HashMap.empty[Class[_], TypeProfile[_]]

    override def getReferenced(reference: Int): Option[AnyRef] = {
        referenceStore.getReferenced(reference)
    }

    override def getReferencedCode(reference: AnyRef): Option[Int] = {
        referenceStore.getReferencedCode(reference)
    }

    override def getProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        val profile = defaultProfiles.get(clazz)
        if (profile.isEmpty) {
            return customProfiles.getOrElse(clazz, {
                val default = newDefaultProfile[T](clazz)
                defaultProfiles.put(clazz, default)
                default
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

    def newDefaultProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
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
        new SimpleTypeProfile[T](clazz, Array(persistence), new ClassMap[ObjectConverter[_ <: T, Any]])
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

    override def putSignature: Boolean = withSignature

    override def useUnsafe: Boolean = unsafeUse
}
