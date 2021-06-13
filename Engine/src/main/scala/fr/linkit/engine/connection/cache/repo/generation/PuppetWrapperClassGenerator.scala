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
import fr.linkit.api.local.system.AppLogger

class PuppetWrapperClassGenerator(center: CompilerCenter, resources: WrappersClassResource) extends PuppetWrapperGenerator {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"
    val requestFactory                  = new WrapperCompilationRequestFactory

    override def getClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]] = {
        getClass[S](PuppetDescription(clazz))
    }

    override def getClass[S](desc: PuppetDescription[S]): Class[S with PuppetWrapper[S]] = {
        val clazz = desc.clazz
        if (clazz.isInterface)
            throw new InvalidPuppetDefException("Provided class is an interface.")
        if (clazz.isArray)
            throw new InvalidPuppetDefException("Provided class is an array.")
        resources
                .findWrapperClass[S](clazz)
                .getOrElse {
                    val result = center.generate {
                        AppLogger.debug(s"Compiling Class Wrapper for class ${clazz.getName}...")
                        requestFactory.makeRequest(desc)
                    }
                    AppLogger.debug(s"Compilation done. (${result.getCompileTime} ms).")
                    result.get.asInstanceOf[Class[S with PuppetWrapper[S]]]
                }
    }

    override def preGenerateClasses[S](defaultLoader: ClassLoader, classes: Seq[Class[_ <: S]]): Unit = {
        preGenerateDescs(defaultLoader, classes.map(PuppetDescription[S]))
    }

    override def preGenerateDescs[S](defaultLoader: ClassLoader, descriptions: Seq[PuppetDescription[S]]): Unit = {
        center.generate {
            requestFactory.makeMultiRequest(descriptions)
        }
    }

    override def isWrapperClassGenerated[S](clazz: Class[S]): Boolean = {
        resources.findWrapperClass[S](clazz).isDefined
    }

    override def isClassGenerated[S <: PuppetWrapper[S]](clazz: Class[S]): Boolean = {
        resources.findWrapperClass[S](clazz).isDefined
    }
}

object PuppetWrapperClassGenerator {

}
