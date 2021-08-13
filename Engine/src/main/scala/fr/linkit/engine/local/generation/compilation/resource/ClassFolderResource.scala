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

package fr.linkit.engine.local.generation.compilation.resource

import fr.linkit.api.connection.cache.obj.generation.GeneratedClassLoader
import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.resource.representation.{FolderRepresentation, ResourceRepresentationFactory}
import fr.linkit.engine.connection.cache.obj.generation.{SyncObjectInstantiationHelper, adaptClassName}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.mapping.ClassMappings

import java.io.File
import java.nio.file.{Files, Path}
import scala.collection.mutable

class ClassFolderResource[C](override val resource: ResourceFolder) extends FolderRepresentation {

    private val folderPath       = Path.of(resource.getAdapter.getAbsolutePath)
    private val generatedClasses = mutable.Map.empty[String, Class[_ <: C]]

    def findClass[S](className: String, loader: ClassLoader): Option[Class[S with C]] = {
        generatedClasses.getOrElse(className, {
            val wrapperClassPath = folderPath.resolve(className.replace('.', File.separatorChar) + ".class")
            if (Files.notExists(wrapperClassPath))
                null
            else {
                val classLoader = new GeneratedClassLoader(folderPath, loader, Seq(classOf[LinkitApplication].getClassLoader))
                val clazz       = Class.forName(className, false, classLoader).asInstanceOf[Class[_ <: C]]
                generatedClasses.put(className, clazz)
                ClassMappings.putClass(clazz)
                clazz
            }
        }) match {
            case clazz: Class[S with C] => Some(clazz)
            case _                      => None
        }
    }

    override protected def initialize(): Unit = ()

}

object ClassFolderResource {
    implicit def factory[A]: ResourceRepresentationFactory[ClassFolderResource[A], ResourceFolder] = {
        new ClassFolderResource[A](_)
    }
}
