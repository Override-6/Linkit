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
import fr.linkit.api.connection.packet.traffic.PacketTraffic
import fr.linkit.engine.connection.packet.persistence.context.PersistenceConfigBuilder.fromScript
import fr.linkit.engine.connection.packet.persistence.context.profile.TypeProfileBuilder
import fr.linkit.engine.connection.packet.persistence.context.script.{PersistenceScriptConfig, ScriptPersistenceConfigHandler}
import fr.linkit.engine.local.script.ScriptExecutor
import fr.linkit.engine.local.utils.ClassMap

import java.net.URL
import scala.reflect.{ClassTag, classTag}

class PersistenceConfigBuilder {

    private val persistors     = new ClassMap[TypePersistence[_ <: AnyRef]]
    private val converters     = new ClassMap[ObjectConverter[_ <: AnyRef, _]]
    private val referenceStore = new WeakReferencedObjectStore

    protected var unsafeUse           = true
    protected var referenceAllObjects = false
    protected var wide                = false

    def this(other: PersistenceConfigBuilder) {
        this()
        transfer(other)
    }

    object profiles {

        private[PersistenceConfigBuilder] val customProfiles = new ClassMap[TypeProfileBuilder[_ <: AnyRef]]()

        def +=[T <: AnyRef : ClassTag](builder: TypeProfileBuilder[T]): this.type = {
            val clazz = classTag[T].runtimeClass
            customProfiles.put(clazz, builder)
            this
        }
    }

    def transfer(other: PersistenceConfigBuilder): this.type = {
        persistors ++= other.persistors
        converters ++= other.converters
        referenceStore ++= other.referenceStore
        unsafeUse = other.unsafeUse
        referenceAllObjects = other.referenceAllObjects
        wide = other.wide
        profiles.customProfiles ++= other.profiles.customProfiles
        this
    }

    def putContextReference(ref: AnyRef): Unit = {
        referenceStore += ref
    }

    def putContextReference(id: Int, ref: AnyRef): Unit = {
        referenceStore += (id, ref)
    }

    def addPersistence[T <: AnyRef : ClassTag](persistence: TypePersistence[T]): this.type = {
        val clazz = classTag[T].runtimeClass
        if (persistence eq null) {
            persistors.remove(clazz)
        } else {
            persistors.put(clazz, persistence)
        }
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
        val clazz = classTag[T].runtimeClass
        if (converter eq null) {
            converters.remove(clazz)
        } else {
            converters.put(clazz, converter)
        }
        this
    }

    def build(context: PersistenceContext): PersistenceConfig = {
        val profiles = collectProfiles()
        transfer(fromScript(getClass.getResource("/default_scripts/persistence_minimal.sc"), context.traffic))
        new SimplePersistenceConfig(context, profiles, referenceStore, unsafeUse, referenceAllObjects, wide)
    }

    private def collectProfiles(): ClassMap[TypeProfile[_]] = {
        val map = profiles.customProfiles

        def cast[X](a: Any): X = a.asInstanceOf[X]

        //noinspection TypeAnnotation
        val tempStore = new TypeProfileStore {
            val cache = new ClassMap[TypeProfile[_]]()

            override def getProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
                cache(clazz).asInstanceOf[TypeProfile[T]]
            }
        }

        persistors.foreachEntry((clazz, persistence) => {
            map.getOrElseUpdate(clazz, new TypeProfileBuilder[AnyRef]())
                    .addPersistence(cast(persistence))
        })
        converters.foreachEntry((clazz, converter) => {
            map.getOrElseUpdate(clazz, new TypeProfileBuilder[AnyRef]())
                    .setTConverter(cast(converter))
        })
        val finalMap = map.toSeq
                .sortBy(pair => getClassHierarchicalDepth(pair._1)) //sorting from Object class to most "far away from Object" classes
                .map(pair => {
                    val clazz   = pair._1
                    val profile = pair._2.build(tempStore)
                    tempStore.cache.put(clazz, profile)
                    (clazz, profile)
                }).toMap
        new ClassMap[TypeProfile[_]](finalMap)
    }

    private def getClassHierarchicalDepth(clazz: Class[_]): Int = {
        var cl    = clazz
        var depth = 0
        while (cl ne null) {
            cl = cl.getSuperclass
            depth += 1
        }
        depth
    }

}

object PersistenceConfigBuilder {

    implicit def autoBuild(context: PersistenceContext, builder: PersistenceConfigBuilder): PersistenceConfig = {
        builder.build(context)
    }

    def fromScript(url: URL, traffic: PacketTraffic): PersistenceConfigBuilder = {
        val application = traffic.application
        val script      = ScriptExecutor.getOrCreateScript[PersistenceScriptConfig](url, application)(ScriptPersistenceConfigHandler)
                .newScript(application, traffic)
        script.execute()
        script
    }

}

