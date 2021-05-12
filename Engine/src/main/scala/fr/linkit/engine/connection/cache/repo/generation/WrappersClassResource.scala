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

import fr.linkit.api.connection.cache.repo.PuppetWrapper
import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.resource.representation.{FolderRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.repo.generation.WrappersClassResource.{ClassesFolder, SourcesFolder, WrapperPackageName, WrapperPrefixName}

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Files, Path, StandardOpenOption}
import javax.tools.ToolProvider
import scala.collection.mutable

class WrappersClassResource(override val resource: ResourceFolder) extends FolderRepresentation {

    private val folderPath           = Path.of(resource.getAdapter.getAbsolutePath)
    private val queuePath            = Path.of(folderPath + SourcesFolder)
    private val generatedClassesPath = Path.of(folderPath + ClassesFolder)
    private val classLoader          = preInit()
    private val generatedClasses     = mutable.Map.empty[String, Class[_ <: PuppetWrapper[Serializable]]]
    initialize()

    private[generation] def addToQueue(className: String, classSource: String): Unit = {
        val classSimpleName = className.drop(className.lastIndexOf('.') + 1)
        val classFolder     = Path.of(className.dropRight(classSimpleName.length).replace('.', File.separatorChar))
        val path            = queuePath.resolve(classFolder + s"\\$WrapperPrefixName" + classSimpleName + ".java")
        if (Files.notExists(path))
            Files.createDirectories(path.getParent)
        Files.writeString(path, classSource, StandardOpenOption.CREATE)
    }

    def getWrapperClass[S <: Serializable](puppetClassName: String): Option[Class[S with PuppetWrapper[S]]] = {
        generatedClasses.getOrElseUpdate(toWrappedClassName(puppetClassName), {
            val wrapperClassPath = generatedClassesPath.resolve(puppetClassName.replace('.', File.separatorChar))
            if (Files.notExists(wrapperClassPath))
                null
            else {
                Class.forName(puppetClassName, false, classLoader).asInstanceOf[Class[_ <: PuppetWrapper[Serializable]]]
            }
        }) match {
            case clazz: Class[S with PuppetWrapper[S]] => Some(clazz)
            case _                                     => None
        }
    }

    def compileQueue(): Unit = {
        val sources = listSources()
        if (sources.isEmpty)
            return

        AppLogger.trace(s"Compiling dynamic wrapper classes ${
            sources
                    .map(pathToClassName(_, 5))
                    .mkString(", ")
        }...")

        val javac   = ToolProvider.getSystemJavaCompiler
        val options = Array[String]("-d", generatedClassesPath.toString, "-Xlint:all") ++ sources
        val code    = javac.run(null, null, null, options: _*)
        if (code != 0)
            throw new InvalidPuppetDefException(s"Javac rejected class queue compilation. See above messages for further details. (error code: $code)")

        listSources()
                .map(WrapperPackageName + pathToClassName(_, 5))
                .foreach(loadWrapperClass)

        //clearQueue()
    }

    private def loadWrapperClass(name: String): Unit = {
        val clazz = classLoader.loadClass(name)
        generatedClasses.put(toWrappedClassName(name), clazz.asInstanceOf[Class[_ <: PuppetWrapper[Serializable]]])
    }

    private def toWrappedClassName(puppetWrapperName: String): String = {
        val pivotIndex = puppetWrapperName.lastIndexOf('.')

        var simpleName = puppetWrapperName.drop(pivotIndex + 1)
        if (!simpleName.startsWith(WrapperPrefixName))
            return puppetWrapperName
        simpleName = simpleName.drop(WrapperPrefixName.length)

        var packageName = puppetWrapperName.take(pivotIndex)
        if (!packageName.startsWith(WrapperPackageName))
            return packageName + simpleName
        packageName = packageName.drop(WrapperPackageName.length)

        packageName + '.' + simpleName
    }

    private def listSources(): Array[String] = {
        def listSources(path: Path): Array[String] = {
            Files.list(path)
                    .toArray(new Array[Path](_))
                    .flatMap(subPath => {
                        if (Files.isDirectory(subPath))
                            listSources(subPath)
                        else Array(subPath.toString)
                    })
                    .filter(_.endsWith(".java"))
        }

        listSources(queuePath)
    }

    def removeFromQueue(className: String): Unit = {
        val path = queuePath.resolve(className)
        Files.deleteIfExists(path)
    }

    override def initialize(): Unit = {

        def loadFolder(folder: Path): Unit = {
            Files.list(folder)
                    .forEach { sub =>
                        if (Files.isDirectory(sub))
                            loadFolder(sub)
                        else if (sub.toString.endsWith(".class")) {
                            loadWrapperClass(pathToClassName(sub.toString, 6))
                        }
                    }
        }

        loadFolder(generatedClassesPath)
        //clearQueue()
    }

    private def preInit(): URLClassLoader = {
        if (classLoader != null)
            throw new IllegalStateException("Resource already initialized !")

        Files.createDirectories(queuePath)
        Files.createDirectories(generatedClassesPath)
        new URLClassLoader(Array(generatedClassesPath.toUri.toURL))
    }

    private def pathToClassName(generatedClassPath: String, suffixLength: Int): String = {
        val parent = if (generatedClassPath.startsWith(generatedClassesPath.toString)) generatedClassesPath else queuePath
        val name   = generatedClassPath.drop(parent.toString.length + 1)
        name.replace(File.separator, ".").dropRight(suffixLength)
    }

    override def close(): Unit = {
        //clearQueue()
    }

    private def clearQueue(): Unit = {
        def deleteFolder(path: Path): Unit = {
            Files.list(path)
                    .forEach(subPath => {
                        if (Files.isDirectory(subPath))
                            deleteFolder(subPath)
                        else Files.delete(subPath)
                    })
        }

        deleteFolder(queuePath)
    }

}

object WrappersClassResource extends ResourceRepresentationFactory[WrappersClassResource, ResourceFolder] {

    val SourcesFolder     : String = "/Sources/"
    val ClassesFolder     : String = "/Classes/"
    val WrapperPrefixName : String = "Puppet"
    val WrapperPackageName: String = "gen."

    override def apply(resource: ResourceFolder): WrappersClassResource = new WrappersClassResource(resource)
}
