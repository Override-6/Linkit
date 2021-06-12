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

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.generation.PuppetWrapperGenerator
import fr.linkit.api.connection.cache.repo.{InvalidPuppetDefException, PuppetWrapper}
import fr.linkit.api.local.generation.compilation.CompilerCenter
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes

class PuppetWrapperClassGenerator(center: CompilerCenter, resources: WrappersClassResource) extends PuppetWrapperGenerator {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"


    override def getClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]] = {
        getClass[S](PuppetDescription(clazz))
    }

    override def getClass[S](desc: PuppetDescription[S]): Class[S with PuppetWrapper[S]] = {
        val clazz = desc.clazz
        if (clazz.isInterface)
            throw new InvalidPuppetDefException("Provided class is an interface.")
        var loader = clazz.getClassLoader
        if (loader == null)
            loader = classOf[PuppetWrapperClassGenerator].getClassLoader //Use the Application classloader
        resources
            .findWrapperClass[S](clazz, loader)
            .getOrElse {
                val (sourceCode, compilerType) = genPuppetClassSourceCode(desc)
                resources.addToQueue(clazz, sourceCode, compilerType)
                resources.compileQueue(loader)
                resources.findWrapperClass[S](clazz, loader).get
            }
    }

    override def preGenerateClasses[S](defaultLoader: ClassLoader, classes: Seq[Class[_ <: S]]): Unit = {
        preGenerateDescs(defaultLoader, classes.map(PuppetDescription[S]))
    }

    override def preGenerateDescs[S](defaultLoader: ClassLoader, descriptions: Seq[PuppetDescription[S]]): Unit = {
        descriptions
            .filter(desc => resources.findWrapperClass(desc.clazz, desc.clazz.getClassLoader).isEmpty)
            .foreach(desc => {
                val (source, compileType) = genPuppetClassSourceCode(desc)
                resources.addToQueue(desc.clazz, source, compileType)
            })
        resources.compileQueue(defaultLoader)
    }

    override def isWrapperClassGenerated[S](clazz: Class[S]): Boolean = {
        resources.findWrapperClass[S](clazz, clazz.getClassLoader).isDefined
    }

    override def isClassGenerated[S <: PuppetWrapper[S]](clazz: Class[S]): Boolean = {
        resources.findWrapperClass[S](clazz, clazz.getClassLoader).isDefined
    }

    private def genPuppetClassSourceCode[S](description: PuppetDescription[S]): (String, CompilerType) = {
        (scbp.toClassSource(description), CommonCompilerTypes.Scalac)
    }
}

object PuppetWrapperClassGenerator {

}
