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

package fr.linkit.engine.gnom.cache.sync.generation.sync

import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefMultiple, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.{InvalidClassDefinitionError, SynchronizedObject}
import fr.linkit.api.gnom.network.statics.StaticsCaller
import fr.linkit.api.gnom.reference.NetworkObject
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsCallerDescription}
import fr.linkit.engine.internal.mapping.ClassMappings

import scala.util.Try

class DefaultSyncClassCenter(center: CompilerCenter, resources: SyncObjectClassResource) extends SyncClassCenter {

    private val requestFactory = new SyncClassCompilationRequestFactory()

    override def getSyncClass[S <: AnyRef](clazz: SyncClassDef): Class[S with SynchronizedObject[S]] = {
        if (classOf[StaticsCaller].isAssignableFrom(clazz.mainClass)) {
            if (clazz.isInstanceOf[SyncClassDefMultiple])
                throw new IllegalArgumentException("Static caller sync class representation can't define interfaces.")
            getSyncClassFromDesc[S](SyncStaticsCallerDescription[S with StaticsCaller](clazz.mainClass).asInstanceOf[SyncStructureDescription[S]])
        } else getSyncClassFromDesc[S](SyncObjectDescription[S](clazz))
    }

    override def getSyncClassFromDesc[S <: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]] = {
        getOrGenClass[S](desc)
    }

    private def getOrGenClass[S <: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]] = desc.specs.synchronized {
        val clazz = desc.specs
        val opt   = resources.findClass[S](clazz)
        if (opt.isDefined) opt.get
        else {
            val genClassFullName = desc.classPackage + '.' + desc.className
            val result           = Try(clazz.mainClass.getClassLoader.loadClass(genClassFullName)).getOrElse(genClass[S](desc))
                    .asInstanceOf[Class[S with SynchronizedObject[S]]]
            if (result == null)
                throw new ClassNotFoundException(s"Could not load generated class '$genClassFullName'")
            result
        }
    }

    private def genClass[S <: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]] = {
        val clazz = desc.specs
        checkClassDefValidity(clazz)
        val result = center.processRequest {
            clazz match {
                case multiple: SyncClassDefMultiple =>
                    AppLoggers.Compilation.info(s"Compiling Sync class for ${clazz.mainClass.getName} with additional interfaces ${multiple.interfaces.mkString(", ")}...")
                case _                              =>
                    AppLoggers.Compilation.info(s"Compiling Sync class for ${clazz.mainClass.getName}...")
            }
            requestFactory.makeRequest(desc)
        }
        AppLoggers.Compilation.info(s"Compilation done. (${result.getCompileTime} ms).")
        val syncClass = result.getValue
                .get
                .asInstanceOf[Class[S with SynchronizedObject[S]]]
        ClassMappings.putClass(syncClass)
        syncClass
    }

    /**
     * Ensures that, if the class extends [[NetworkObject]],
     * it explicitly defines the `reference: T` method of the interface.
     * @param clazz
     */
    //TODO explain the error further
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
                           |Error: $cl does not defines method `T reference()`.
                           |It turns out that the Linkit object synchronization class generator met a class that cannot be handled...
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
        result.getValue.get.foreach(ClassMappings.putClass)
        val ct = result.getCompileTime
        AppLoggers.Compilation.info(s"Compilation done in $ct ms.")
    }

    override def isClassGenerated(classDef: SyncClassDef): Boolean = {
        resources.findClass[AnyRef](classDef).isDefined
    }

}