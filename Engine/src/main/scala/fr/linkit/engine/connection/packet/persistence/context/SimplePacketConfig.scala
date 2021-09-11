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
import scala.reflect.{ClassTag, classTag}

abstract class SimplePacketConfig(persistenceConfig: PersistenceContext) extends PacketConfig {

    override def getReferenced(reference: Int): Option[AnyRef] = {
        references.codeToRef.get(reference)
    }

    override def getReferencedCode(reference: AnyRef): Option[Int] = {
        references.refToCode.get(reference)
    }

    override def getProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        profiles.customProfiles.getOrElse(clazz, newDefaultProfile[T](clazz))
                .asInstanceOf[TypeProfile[T]]
    }

    override def informObjectReceived(obj: AnyRef): Unit = {
        informObject(obj)
    }

    override def informObjectSent(obj: AnyRef): Unit = {
        informObject(obj)
    }

    @inline
    private def informObject(obj: AnyRef): Unit = {
        if (referenceAllObjects) references += obj
    }

    def newDefaultProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
        val constructor                     = persistenceConfig.findConstructor[T](clazz)
        val persistence: TypePersistence[T] = {
            if (classOf[Deconstructive].isAssignableFrom(clazz)) {
                constructor.fold(new DeconstructiveTypePersistence[T with Deconstructive](clazz)) {
                    ctr => new DeconstructiveTypePersistence[T with Deconstructive](clazz, ctr.asInstanceOf[java.lang.reflect.Constructor[T with Deconstructive]])
                }
            }.asInstanceOf[TypePersistence[T]] else {
                val deconstructor = persistenceConfig.findDeconstructor[T](clazz)
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
        new SimpleTypeProfile[T](clazz, Array(persistence), new ClassMap[ObjectConverter[T, Any]])
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

    object profiles {

        private[SimplePacketConfig] val customProfiles = mutable.HashMap.empty[Class[_], TypePersistence[_]]

        def +=[T: ClassTag](profile: TypePersistence[T]): this.type = {
            val clazz = classTag[T].runtimeClass
            customProfiles.put(clazz, profile)
            this
        }
    }

    object references {

        private[SimplePacketConfig] val codeToRef = mutable.WeakHashMap.empty[Int, AnyRef]
        private[SimplePacketConfig] val refToCode = mutable.WeakHashMap.empty[AnyRef, Int]

        def ++=(refs: AnyRef*): this.type = {
            refs.foreach(+=)
            this
        }

        def +=(anyRef: AnyRef): this.type = {
            +=(anyRef.hashCode(), anyRef)
        }

        def +=(code: Int, anyRef: AnyRef): this.type = {
            codeToRef.put(code, anyRef)
            refToCode.put(anyRef, code)
            this
        }
    }

    protected var unsafeUse     = true
    protected var withSignature       = true
    protected var referenceAllObjects = false
    protected var wide                = false

    override def useUnsafe: Boolean = unsafeUse

    override def putSignature: Boolean = withSignature

    override def widePacket: Boolean = wide

}
