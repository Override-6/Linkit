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

package fr.linkit.engine.gnom.cache.sync.generation

import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefMultiple, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.generation.{GeneratedClassLoader, SyncClassCenter}
import fr.linkit.api.gnom.cache.sync.{InvalidClassDefinitionError, SynchronizedObject}
import fr.linkit.api.gnom.network.statics.StaticsCaller
import fr.linkit.api.gnom.referencing.NetworkObject
import fr.linkit.api.internal.compilation.CompilerCenter
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsCallerDescription}
import fr.linkit.engine.internal.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.mapping.ClassMappings

import scala.util.Try

class DefaultSyncClassCenter(storage: SyncClassStorageResource, center: CompilerCenter = DefaultCompilerCenter) extends SyncClassCenter {

    private val requestFactory = SyncClassCompilationRequestFactory

    override def getSyncClass[S <: AnyRef](clazz: SyncClassDef): Class[S with SynchronizedObject[S]] = {
        if (classOf[StaticsCaller].isAssignableFrom(clazz.mainClass)) {
            if (clazz.isInstanceOf[SyncClassDefMultiple])
                throw new IllegalArgumentException("Static caller sync class representation can't define interfaces.")
            getSyncClassFromDesc[S](SyncStaticsCallerDescription[S with StaticsCaller](clazz.mainClass).asInstanceOf[SyncStructureDescription[S]])
        } else getSyncClassFromDesc[S](SyncObjectDescription[S](clazz))
    }

    override def getSyncClassFromDesc[S <: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]] = {
        desc match {
            case desc: SyncObjectDescription[S] =>
                getOrGenClass[S](desc)
            case _                              =>
                throw new IllegalArgumentException("desc is not an object structure description.")

        }
    }

    private def getOrGenClass[S <: AnyRef](desc: SyncObjectDescription[S]): Class[S with SynchronizedObject[S]] = desc.specs.synchronized {
        val classDef = desc.specs
        classDef.ensureOverrideable()
        val opt = storage.findClass[S](classDef)
        if (opt.isDefined) opt.get
        else {
            val genClassFullName = desc.classPackage + '.' + desc.className
            val result           = Try(classDef.mainClass.getClassLoader.loadClass(genClassFullName)).getOrElse(genClass[S](desc))
                                                                                                     .asInstanceOf[Class[S with SynchronizedObject[S]]]
            if (result == null)
                throw new ClassNotFoundException(s"Could not load generated class '$genClassFullName'")
            result
        }
    }

    private def genClass[S <: AnyRef](desc: SyncObjectDescription[S]): Class[S with SynchronizedObject[S]] = {
        val classDef = desc.specs
        checkClassDefValidity(classDef)
        val result = center.processRequest {
            classDef match {
                case multiple: SyncClassDefMultiple =>
                    AppLoggers.Compilation.info(s"Compiling Sync class for ${classDef.mainClass.getName} with additional interfaces ${multiple.interfaces.mkString(", ")}...")
                case _                              =>
                    AppLoggers.Compilation.info(s"Compiling Sync class for ${classDef.mainClass.getName}...")
            }
            val workingDir = storage.resource.getPath.getParent
            requestFactory.makeRequest(desc, workingDir)
        }
        AppLoggers.Compilation.info(s"Compilation done. (${result.getCompileTime} ms).")
        val syncClass = result.getClasses
                              .head
                              .asInstanceOf[Class[S with SynchronizedObject[S]]]
        ClassMappings.putClass(syncClass)
        syncClass
    }

    /**
     * Ensures that, if the class extends [[NetworkObject]],
     * it explicitly defines the `reference: T` method of the interface.
     *
     * @param clazz
     */
    private def checkClassDefValidity(specs: SyncClassDef): Unit = {
        checkClassValidity(specs.mainClass)
        specs match {
            case multiple: SyncClassDefMultiple =>
                multiple.interfaces.foreach(checkClassValidity)
            case _                              =>
        }
    }

    private def checkClassValidity(clazz: Class[_]): Unit = {
        if (!classOf[NetworkObject[_]].isAssignableFrom(clazz))
            return

        ensureClassValidity(clazz, Array())

        def ensureClassValidity(cl: Class[_], path: Array[Class[_]]): Unit = {
            val interfaces = cl.getInterfaces
            if (!interfaces.contains(classOf[NetworkObject[_]]))
                interfaces.foreach(ensureClassValidity(_, path :+ cl))
            try {
                cl.getDeclaredMethod("reference")
            } catch {
                case _: NoSuchMethodException =>
                    val classHierarchyPath = (path :+ cl).map(_.getSimpleName).mkString(" extends ")
                    throw new InvalidClassDefinitionError(
                        s"""
                           |
                           |Error: $cl does not explicitely defines method `T reference()`.
                           |It turns out that the proxy class generator met a class that cannot be handled...
                           |What turned wrong ?
                           |$clazz extends $cl ($classHierarchyPath):
                           |$cl is directly implementing ${classOf[NetworkObject[_]]}, but does not explicitly defines method `T reference()`.
                           |Please declare the requested method in `$cl`, recompile the project then retry compiling ${cl}Sync.
                           |
                           |""".stripMargin)
            }
        }
    }

    override def preGenerateClasses(classes: Seq[SyncClassDef]): Unit = {
        val toCompile = classes.filterNot(isClassGenerated)
        if (toCompile.isEmpty)
            return
        toCompile.foreach(checkClassDefValidity)
        val result = center.processRequest {
            AppLoggers.Compilation.info(s"Compiling Sync Classes for ${toCompile.map(_.mainClass.getName).mkString(", ")}...")
            requestFactory.makeMultiRequest(toCompile.map(SyncObjectDescription(_)))
        }
        result.getClasses.foreach(ClassMappings.putClass)
        val ct = result.getCompileTime
        AppLoggers.Compilation.info(s"Compilation done in $ct ms.")
    }

    override def isClassGenerated(classDef: SyncClassDef): Boolean = {
        storage.findClass[AnyRef](classDef).isDefined
    }

}