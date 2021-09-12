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
import fr.linkit.engine.connection.packet.persistence.context.profile.TypeProfileBuilder
import fr.linkit.engine.local.utils.ClassMap

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class PacketConfigBuilder {

    private val persistors     = new ClassMap[TypePersistence[_ <: AnyRef]]
    private val converters     = new ClassMap[ObjectConverter[_ <: AnyRef, _]]
    private val referenceStore = new WeakReferencedObjectStore

    protected var unsafeUse           = true
    protected var withSignature       = true
    protected var referenceAllObjects = false
    protected var wide                = false

    object profiles {

        private[PacketConfigBuilder] val customProfiles = mutable.HashMap.empty[Class[_], TypeProfile[_]]

        def +=[T <: AnyRef : ClassTag](profile: TypeProfile[T]): this.type = {
            val clazz = classTag[T].runtimeClass
            customProfiles.put(clazz, profile)
            this
        }
    }

    def putContextReference(ref: AnyRef): Unit = {
        referenceStore += ref
    }

    def putContextReference(id: Int, ref: AnyRef): Unit = {
        referenceStore += (id, ref)
    }

    def addPersistence[T <: AnyRef : ClassTag](persistence: TypePersistence[T]): this.type = {
        persistors.put(classTag[T].runtimeClass, persistence)
        this
    }

    def setTConverter[T <: AnyRef : ClassTag, B](converter: ObjectConverter[T, B]): this.type = {
        setTConverter0[T, B](converter)
    }

    def setTNewConverter[T <: AnyRef : ClassTag, B](fTo: T => B)(fFrom: B => T): this.type = {
        setTConverter0[T, B](new ObjectConverter[T, B] {
            override def to(t: T): B = fTo(t)

            override def from(b: B): T = fFrom(b)
        })
    }

    private def setTConverter0[T <: AnyRef : ClassTag, B](converter: ObjectConverter[T, B]): this.type = {
        converters.put(classTag[T].runtimeClass, converter)
        this
    }

    def build(context: PersistenceContext): PacketConfig = {
        val profiles = collectProfiles()
        new SimplePacketConfig(context, profiles, referenceStore, unsafeUse, withSignature, referenceAllObjects, wide)
    }

    private def collectProfiles(): ClassMap[TypeProfile[_]] = {
        val map = profiles.customProfiles.toMap
        def cast[X](a: Any): X = a.asInstanceOf[X]
        val otherProfiles = mutable.HashMap.empty[Class[_], TypeProfileBuilder[_ <: AnyRef]]
        persistors.foreachEntry((clazz, persistence) => {
            otherProfiles.getOrElseUpdate(clazz, new TypeProfileBuilder[AnyRef]())
                    .addPersistence(cast(persistence))
        })
        converters.foreachEntry((clazz, converter) => {
            otherProfiles.getOrElseUpdate(clazz, new TypeProfileBuilder[AnyRef]())
                    .setTConverter(cast(converter))
        })
        val finalMap = otherProfiles.view.mapValues(_.build()).toMap ++ map
        new ClassMap[TypeProfile[_]](finalMap)
    }

}

object PacketConfigBuilder {
    implicit def autoBuild(context: PersistenceContext, builder: PacketConfigBuilder): PacketConfig = {
        builder.build(context)
    }
}

