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

import fr.linkit.api.gnom.cache.sync.contract.description.SyncStructureDescription
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.{InvalidClassDefinitionError, InvalidSyncClassRequestException, SynchronizedObject}
import fr.linkit.api.gnom.reference.NetworkObject
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsCallerDescription}
import fr.linkit.engine.gnom.network.statics.StaticsCaller
import fr.linkit.engine.internal.mapping.ClassMappings

import scala.util.Try

class DefaultSyncClassCenter(center: CompilerCenter, resources: SyncObjectClassResource) extends SyncClassCenter {

    private val requestFactory = new SyncClassCompilationRequestFactory(center)

    override def getSyncClass[S <: AnyRef](clazz: Class[_]): Class[S with SynchronizedObject[S]] = {
        if (classOf[StaticsCaller].isAssignableFrom(clazz)) {
            getSyncClassFromDesc[S](SyncStaticsCallerDescription[S with StaticsCaller](clazz).asInstanceOf[SyncStructureDescription[S]])
        } else getSyncClassFromDesc[S](SyncObjectDescription[S](clazz))
    }

    override def getSyncClassFromDesc[S <: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]] = {
        val clazz = desc.clazz
        if (clazz.isArray)
            throw new InvalidSyncClassRequestException(s"Provided class is Array. ($clazz)")
        if (!isOverrideable(clazz.getModifiers))
            throw new InvalidSyncClassRequestException(s"Provided class is not extensible. ($clazz)")
        getOrGenClass[S](desc)
    }

    private def isOverrideable(mods: Int): Boolean = {
        import java.lang.reflect.Modifier._
        !isFinal(mods) && isPublic(mods)
    }

    private def getOrGenClass[S <: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]] = desc.clazz.synchronized {
        val clazz = desc.clazz
        val opt   = resources.findClass[S](clazz)
        if (opt.isDefined) opt.get
        else {
            val genClassFullName = desc.classPackage + '.' + desc.className
            val result           = Try(clazz.getClassLoader.loadClass(genClassFullName)).getOrElse(genClass[S](desc))
                    .asInstanceOf[Class[S with SynchronizedObject[S]]]
            if (result == null)
                throw new ClassNotFoundException(s"Could not load generated class '$genClassFullName'")
            result
        }
    }

    private def genClass[S <: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]] = {
        val clazz = desc.clazz
        checkClassValidity(clazz)
        val result = center.processRequest {
            AppLogger.info(s"Compiling Sync class for ${clazz.getName}...")
            requestFactory.makeRequest(desc)
        }
        AppLogger.info(s"Compilation done. (${result.getCompileTime} ms).")
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

    override def preGenerateClasses(classes: Seq[Class[_]]): Unit = {
        val toCompile = classes.filterNot(isClassGenerated)
        if (toCompile.isEmpty)
            return
        toCompile.foreach(checkClassValidity)
        val result = center.processRequest {
            AppLogger.info(s"Compiling Sync Classes for ${toCompile.map(_.getSimpleName).mkString(", ")}...")
            requestFactory.makeMultiRequest(toCompile.map(SyncObjectDescription(_)))
        }
        result.getValue.get.foreach(ClassMappings.putClass)
        val ct = result.getCompileTime
        AppLogger.info(s"Compilation done in $ct ms.")
    }

    override def isClassGenerated(clazz: Class[_]): Boolean = {
        resources.findClass[AnyRef](clazz).isDefined
    }

}

object DefaultSyncClassCenter {

}
