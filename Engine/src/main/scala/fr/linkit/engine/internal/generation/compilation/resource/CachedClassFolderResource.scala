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

package fr.linkit.engine.internal.generation.compilation.resource

import fr.linkit.api.application.resource.local.ResourceFolder
import fr.linkit.api.application.resource.representation.ResourceRepresentationFactory
import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.internal.generation.resource.ClassFolderResource
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.internal.mapping.ClassMappings

import java.io.File
import java.nio.file.Files
import scala.collection.mutable

class CachedClassFolderResource[C](override val resource: ResourceFolder) extends ClassFolderResource[C] {
    
    private val folderPath       = resource.getPath
    private val generatedClasses = mutable.Map.empty[String, Class[_ <: C]]
    
    override def findClass[S <: AnyRef](className: String, loader: ClassLoader): Option[Class[S with C]] = {
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

object CachedClassFolderResource {
    
    implicit def apply[A]: ResourceRepresentationFactory[CachedClassFolderResource[A], ResourceFolder] = {
        new CachedClassFolderResource[A](_)
    }
}
