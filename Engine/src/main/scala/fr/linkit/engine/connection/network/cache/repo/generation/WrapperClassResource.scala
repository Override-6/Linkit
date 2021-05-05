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

package fr.linkit.engine.connection.network.cache.repo.generation

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Files, Path, StandardOpenOption}

import fr.linkit.api.connection.network.cache.repo.PuppetWrapper
import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.resource.representation.FolderRepresentation
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.network.cache.repo.generation.WrapperClassResource.{ClassesFolder, SourcesFolder}
import fr.linkit.engine.local.mapping.ClassMappings

import scala.collection.mutable

class WrapperClassResource(override val folder: ResourceFolder) extends FolderRepresentation {

    private val folderPath           = Path.of(folder.getAdapter.getAbsolutePath)
    private val queuePath            = folderPath.resolve(SourcesFolder)
    private val generatedClassesPath = folderPath.resolve(ClassesFolder)
    private val classLoader          = new URLClassLoader(Array(generatedClassesPath.toUri.toURL))
    private val generatedClasses     = mutable.Map.empty[String, Class[_ <: PuppetWrapper[Serializable]]]

    def addToQueue(className: String, classSource: String): Unit = {
        val path = queuePath.resolve(className + ".java")
        Files.writeString(path, classSource, StandardOpenOption.CREATE)
    }

    def getWrapperClass[S <: Serializable](puppetClassName: String): Option[Class[S with PuppetWrapper[S]]] = {
        generatedClasses.getOrElseUpdate(puppetClassName, {
            val wrapperClassPath = generatedClassesPath.resolve(puppetClassName.replace('.', File.separatorChar))
            if (Files.notExists(wrapperClassPath))
                null
            else {
                classLoader.loadClass(puppetClassName).asInstanceOf[Class[_ <: PuppetWrapper[Serializable]]]
            }
        }) match {
            case clazz: Class[S with PuppetWrapper[S]] => Some(clazz)
            case _                                     => None
        }
    }

    def compileQueueAndClear(): Unit = {
        val classPaths   = ClassMappings.getSources.map(source => '\"' + source.getLocation.getPath.drop(1) + '\"').toString()
        val javacProcess = new ProcessBuilder("javac", "-d", queuePath.toString, "-Xlint:all", s"-cp", classPaths, generatedClassesPath.toString)
        javacProcess.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        javacProcess.redirectError(ProcessBuilder.Redirect.INHERIT)

        val classes = Files.list(queuePath).map(_.getFileName).toArray()
        AppLogger.debug(s"Compiling Puppet classes '${classes.mkString(", ")}'")
        val exitValue = javacProcess.start().waitFor()
        if (exitValue != 0)
            throw new InvalidPuppetDefException(s"Javac rejected compilation. Check above error prints for further details.")
        AppLogger.debug(s"Compilation done.")
        clearQueue()
    }

    def removeFromQueue(className: String): Unit = {
        val path = queuePath.resolve(className)
        Files.deleteIfExists(path)
    }

    def initialize(): Unit = {
        Files.createDirectories(queuePath)
        clearQueue()
    }

    override def close(): Unit = {
        clearQueue()
    }

    private def clearQueue(): Unit = {
        Files.list(queuePath)
                .forEach(element => Files.deleteIfExists(element))
    }

}

object WrapperClassResource {

    val SourcesFolder: String = "/sources/"
    val ClassesFolder: String = "/classes/"
}
