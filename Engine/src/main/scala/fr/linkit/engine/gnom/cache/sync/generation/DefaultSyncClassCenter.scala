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

package fr.linkit.engine.gnom.cache.sync.generation

import fr.linkit.api.gnom.cache.sync.description.SyncObjectSuperclassDescription
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.{InvalidPuppetDefException, SynchronizedObject}
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.description.SimpleSyncObjectSuperClassDescription
import fr.linkit.engine.internal.mapping.ClassMappings

class DefaultSyncClassCenter(center: CompilerCenter, resources: SyncObjectClassResource) extends SyncClassCenter {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"
    val requestFactory                  = new SyncClassCompilationRequestFactory

    override def getSyncClass[S](clazz: Class[S]): Class[S with SynchronizedObject[S]] = {
        getSyncClassFromDesc[S](SimpleSyncObjectSuperClassDescription[S](clazz))
    }

    override def getSyncClassFromDesc[S](desc: SyncObjectSuperclassDescription[S]): Class[S with SynchronizedObject[S]] = {
        val clazz = desc.clazz
        if (clazz.isInterface)
            throw new InvalidPuppetDefException("Provided class is abstract.")
        if (clazz.isArray)
            throw new InvalidPuppetDefException("Provided class is an Array.")
        getOrGenClass[S](desc)
    }

    private def getOrGenClass[S](desc: SyncObjectSuperclassDescription[S]): Class[S with SynchronizedObject[S]] = desc.clazz.synchronized {
        val clazz = desc.clazz
        val opt   = resources
                .findClass[S](clazz)
        if (opt.isDefined)
            opt.get
        else {
            val result = center.processRequest {
                AppLogger.debug(s"Compiling Sync class for ${clazz.getName}...")
                requestFactory.makeRequest(desc)
            }
            AppLogger.debug(s"Compilation done. (${result.getCompileTime} ms).")
            val puppetClass = result.getResult
                    .get
                    .asInstanceOf[Class[S with SynchronizedObject[S]]]
            ClassMappings.putClass(puppetClass)
            puppetClass
        }
    }

    override def preGenerateClasses(classes: Seq[Class[_]]): Unit = {
        preGenerateClasses(classes.map(SimpleSyncObjectSuperClassDescription(_)).toList)
    }

    override def preGenerateClasses(descriptions: List[SyncObjectSuperclassDescription[_]]): Unit = {
        val toCompile = descriptions.filter(desc => resources.findClass(desc.clazz).isEmpty)
        if (toCompile.isEmpty)
            return
        val ct = center.processRequest {
            AppLogger.debug(s"Compiling Wrapper Classes for ${toCompile.map(_.clazz.getSimpleName).mkString(", ")}...")
            requestFactory.makeMultiRequest(toCompile)
        }.getCompileTime
        AppLogger.debug(s"Compilation done in $ct ms.")
    }

    override def isWrapperClassGenerated[S](clazz: Class[S]): Boolean = {
        resources.findClass[S](clazz).isDefined
    }

    override def isClassGenerated[S <: SynchronizedObject[S]](clazz: Class[S]): Boolean = {
        resources.findClass[S](clazz).isDefined
    }
}

object DefaultSyncClassCenter {

}
