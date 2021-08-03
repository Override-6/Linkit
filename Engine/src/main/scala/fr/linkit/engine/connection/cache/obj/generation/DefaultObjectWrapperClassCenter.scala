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

package fr.linkit.engine.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperClassCenter
import fr.linkit.api.connection.cache.obj.{InvalidPuppetDefException, PuppetWrapper}
import fr.linkit.api.local.generation.PuppetClassDescription
import fr.linkit.api.local.generation.compilation.CompilerCenter
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.description.SimplePuppetClassDescription
import fr.linkit.engine.local.mapping.ClassMappings

class DefaultObjectWrapperClassCenter(center: CompilerCenter, resources: WrappersClassResource) extends ObjectWrapperClassCenter {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"
    val requestFactory                  = new WrapperCompilationRequestFactory

    override def getWrapperClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]] = {
        getWrapperClass[S](SimplePuppetClassDescription[S](clazz))
    }

    override def getWrapperClass[S](desc: PuppetClassDescription[S]): Class[S with PuppetWrapper[S]] = {
        val clazz = desc.clazz
        if (clazz.isInterface)
            throw new InvalidPuppetDefException("Provided class is abstract.")
        if (clazz.isArray)
            throw new InvalidPuppetDefException("Provided class is an Array.")
        resources
            .findWrapperClass[S](clazz)
            .getOrElse {
                val result = center.generate {
                    AppLogger.debug(s"Compiling Wrapper class for $clazz...")
                    requestFactory.makeRequest(desc)
                }
                AppLogger.debug(s"Compilation done. (${result.getCompileTime} ms).")
                val puppetClass = result.getResult
                        .get
                        .asInstanceOf[Class[S with PuppetWrapper[S]]]
                ClassMappings.putClass(puppetClass)
                puppetClass
            }
    }

    override def preGenerateClasses(classes: Seq[Class[_]]): Unit = {
        preGenerateClasses(classes.map(SimplePuppetClassDescription(_)).toList)
    }

    override def preGenerateClasses(descriptions: List[PuppetClassDescription[_]]): Unit = {
        val toCompile = descriptions.filter(desc => resources.findWrapperClass(desc.clazz).isEmpty)
        if (toCompile.isEmpty)
            return
        val ct = center.generate {
            AppLogger.debug(s"Compiling Wrapper Classes for ${toCompile.map(_.clazz.getSimpleName).mkString(", ")}...")
            requestFactory.makeMultiRequest(toCompile)
        }.getCompileTime
        AppLogger.debug(s"Compilation done in $ct ms.")
    }

    override def isWrapperClassGenerated[S](clazz: Class[S]): Boolean = {
        resources.findWrapperClass[S](clazz).isDefined
    }

    override def isClassGenerated[S <: PuppetWrapper[S]](clazz: Class[S]): Boolean = {
        resources.findWrapperClass[S](clazz).isDefined
    }
}

object DefaultObjectWrapperClassCenter {

}