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

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.network.tag.EngineSelector
import fr.linkit.api.gnom.persistence.context._
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.api.gnom.referencing.linker.ContextObjectLinker
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.persistence.config.profile.TypeProfileBuilder
import fr.linkit.engine.gnom.persistence.config.profile.persistence.GenericTypePersistor
import fr.linkit.engine.gnom.persistence.config.script.{PersistenceScriptConfig, ScriptPersistenceConfigHandler}
import fr.linkit.engine.gnom.persistence.config.structure.ArrayObjectStructure
import fr.linkit.engine.gnom.referencing.linker.NodeContextObjectLinker
import fr.linkit.engine.internal.manipulation.creation.ObjectCreator
import fr.linkit.engine.internal.script.ScriptExecutor
import fr.linkit.engine.internal.util.{ClassMap, Identity, ScalaUtils}
import org.jetbrains.annotations.Nullable

import java.lang.reflect.Modifier
import java.net.URL
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class PersistenceConfigBuilder {

    private val persistors     = new ClassMap[TypePersistor[_ <: AnyRef]]
    private val referenceStore = mutable.HashMap.empty[Int, AnyRef]

    def this(other: PersistenceConfigBuilder) {
        this()
        transfer(other)
    }

    object profiles {

        private[PersistenceConfigBuilder] val customProfiles = new mutable.HashMap[Class[_], TypeProfileBuilder[_ <: AnyRef]]()

        def +=[T <: AnyRef : ClassTag](builder: TypeProfileBuilder[T]): this.type = {
            val clazz = classTag[T].runtimeClass
            customProfiles.put(clazz, builder)
            this
        }
    }

    private[gnom] def forEachRefs(action: (Int, AnyRef) => Unit): Unit = {
        referenceStore.foreachEntry(action)
    }

    def transfer(other: PersistenceConfigBuilder): this.type = {
        persistors ++= other.persistors
        referenceStore ++= other.referenceStore
        profiles.customProfiles ++= other.profiles.customProfiles
        this
    }

    def bindConstants[C: ClassTag]: Unit = {
        val clazz = classTag[C].runtimeClass
        clazz.getDeclaredFields.foreach { field =>
            val mods = field.getModifiers
            if (Modifier.isStatic(mods) && Modifier.isFinal(mods))
                bindReference(field.hashCode, field.get(null))
        }
    }

    def setReplacement[A <: AnyRef : ClassTag](fTo: A => AnyRef): this.type = {
        persistors put(classTag[A].runtimeClass, new ReplacementPersistor(fTo))
        this
    }

    def setTConverter[A <: AnyRef : ClassTag, B: ClassTag](fTo: A => B)(fFrom: B => A, procrastinator: => Procrastinator = null): this.type = {
        val fromClass = classTag[A].runtimeClass
        val toClass   = classTag[B].runtimeClass

        persistors put(fromClass, new DecompositionPersistor(procrastinator)(toClass, fTo, fFrom))
        this
    }

    def putContextReference(ref: AnyRef): Unit = {
        referenceStore put(ref.hashCode(), ref)
    }

    def bindReference(id: Int, ref: AnyRef): Unit = {
        referenceStore put(id, ref)
    }

    def putContextReference(id: Int, ref: Identity[AnyRef]): Unit = {
        referenceStore put(id, ref)
    }

    def putPersistence[T <: AnyRef : ClassTag](persistence: TypePersistor[T]): this.type = {
        val clazz = classTag[T].runtimeClass
        if (persistence eq null) {
            persistors.remove(clazz)
        } else {
            persistors.put(clazz, persistence)
        }
        this
    }

    def build(@Nullable storeParent: ContextObjectLinker,
              omc                  : ObjectManagementChannel,
              selector             : EngineSelector): PersistenceConfig = {
        if (omc == null)
            throw new NullPointerException("ObjectManagementChannel is null")
        build(new NodeContextObjectLinker(storeParent, omc, selector))
    }

    private[linkit] def build(linker: ContextObjectLinker): PersistenceConfig = {
        var config: PersistenceConfig = null
        val store                     = new TypeProfileStore {
            @inline private def check(): Unit = if (config eq null) throw new IllegalStateException("config not initialized")

            override def getProfile[T <: AnyRef](clazz: Class[_]): TypeProfile[T] = {
                check()
                config.getProfile[T](clazz)
            }

            override def getProfile[T <: AnyRef](ref: T): TypeProfile[T] = {
                check()
                config.getProfile(ref)
            }

        }
        val profiles                  = collectProfiles(store)
        linker ++= referenceStore.toMap
        config = new SimplePersistenceConfig(profiles, linker)
        config
    }

    private def collectProfiles(store: TypeProfileStore): ClassMap[TypeProfile[_]] = {
        val profiles = this.profiles.customProfiles

        def cast[X](a: Any): X = a.asInstanceOf[X]

        persistors.foreachEntry((clazz, persistence) => {
            profiles.getOrElseUpdate(clazz, new TypeProfileBuilder()(ClassTag(clazz)))
                    .addPersistor(cast(persistence))
        })
        //TODO maybe add an option to let the user choose if it wants a emergency GenericTypePersistor if its persistor did not worked
        profiles.foreachEntry((clazz, profile) => profile.addPersistor(new GenericTypePersistor(clazz)))
        val finalMap = profiles.toSeq
                .sortBy(pair => getClassHierarchicalDepth(pair._1)) //sorting from Object class to most "far away from Object" classes
                .map(pair => {
                    val clazz   = pair._1
                    val profile = pair._2.build(store)
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

    private class ReplacementPersistor[A](fTo: A => AnyRef) extends TypePersistor[A] {
        private def err: Nothing = throw new UnsupportedOperationException("Cannot use replacement persistor for deserialization")

        override val structure: ObjectStructure = new ObjectStructure {
            override def isAssignable(args: Array[Class[_]], from: Int, to: Int): Boolean = false

            override def isAssignable(args: Array[Any], from: Int, to: Int): Boolean = false
        }

        override def initInstance(allocatedObject: A, args: Array[Any], box: ControlBox): Unit = err

        override def transform(t: A): ObjectTranform = Replaced(fTo(t))
    }

    private class DecompositionPersistor[A, B](procrastinator: Procrastinator)(toClass: Class[_], fTo: A => B, fFrom: B => A) extends TypePersistor[A] {
        private def fields(clazz: Class[_]) = ScalaUtils.retrieveAllFields(clazz).filterNot(f => Modifier.isTransient(f.getModifiers))

        override val structure: ObjectStructure = new ArrayObjectStructure {
            override val types: Array[Class[_]] = Array(toClass)
        }


        override def initInstance(allocatedObject: A, args: Array[Any], box: ControlBox): Unit = {
            if (procrastinator eq null)
                initInstance0(allocatedObject, args)
            else {
                box.warpTask(procrastinator) {
                    initInstance0(allocatedObject, args)
                }
            }
        }

        private def initInstance0(allocatedObject: A, args: Array[Any]): Unit = {
            val t: B        = args.head.asInstanceOf[B]
            val from        = fFrom(t)
            val classFields = fields(from.getClass)
            val values      = ObjectCreator.getAllFields(from, classFields)
            ObjectCreator.pasteAllFields(allocatedObject, classFields, values)
        }

        override def transform(t: A): ObjectTranform = {
            val to = fTo(t)
            Decomposition(Array(to))
        }
    }
}


object PersistenceConfigBuilder {

    def fromScript(url: URL, connection: ConnectionContext): PersistenceConfigBuilder = {
        val application = connection.getApp
        val script      = ScriptExecutor
                .getOrCreateScript[PersistenceScriptConfig](url, application)(ScriptPersistenceConfigHandler)
                .newScript(connection)
        script.execute()
        script
    }

}

