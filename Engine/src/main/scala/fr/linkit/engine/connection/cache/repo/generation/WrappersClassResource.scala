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

import fr.linkit.api.connection.cache.repo.generation.{CompilerAccess, GeneratedClassClassLoader}
import fr.linkit.api.connection.cache.repo.{InvalidPuppetDefException, PuppetWrapper}
import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.resource.representation.{FolderRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.repo.generation.WrappersClassResource.{ClassesFolder, SourcesFolder, WrapperPackage, WrapperPackageName, WrapperPrefixName}
import fr.linkit.engine.local.mapping.ClassMappings

import java.io.File
import java.nio.file.{Files, Path, StandardOpenOption}
import javax.tools.ToolProvider
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

//FIXME Critical bug ! This naming system currently can't handle nested / anonymous classes !
class WrappersClassResource(override val resource: ResourceFolder) extends FolderRepresentation {

    private val folderPath           = Path.of(resource.getAdapter.getAbsolutePath)
    private val queuePath            = Path.of(s"$folderPath/$SourcesFolder/${WrapperPackageName}")
    private val generatedClassesPath = Path.of(folderPath + ClassesFolder)
    private val generatedClasses     = mutable.Map.empty[String, Class[_ <: PuppetWrapper[AnyRef]]]
    private val compilersAccess      = ListBuffer.from[CompilerAccess](Array(JavacCompilerAccess))
    initialize()

    def addCompilerAccess(access: CompilerAccess): Unit = compilersAccess += access

    private[generation] def addToQueue(wrappedClass: Class[_], classSource: String): Unit = {
        val wrapperClassName = toWrapperClassName(wrappedClass)
        val classSimpleName  = wrapperClassName.drop(wrapperClassName.lastIndexOf('.') + 1)
        val classFolderPath  = wrapperClassName.drop(WrapperPackageName.length).dropRight(classSimpleName.length)
        val classFolder      = classFolderPath.replace('.', File.separatorChar)
        val path             = Path.of(s"$queuePath/$classFolder/$classSimpleName.java")
        if (Files.notExists(path))
            Files.createDirectories(path.getParent)
        Files.writeString(path, classSource, StandardOpenOption.CREATE)
    }

    private def toWrapperClassName(wrappedClass: Class[_]): String = {
        WrapperPackage + wrappedClass.getPackageName + '.' + WrapperPrefixName + wrappedClass.getSimpleName
    }

    def findWrapperClass[S](puppetClass: Class[_], parent: ClassLoader): Option[Class[S with PuppetWrapper[S]]] = {
        val puppetClassName  = puppetClass.getName
        val wrapperClassName = toWrapperClassName(puppetClassName)
        generatedClasses.getOrElseUpdate(wrapperClassName, {
            val wrapperClassPath = generatedClassesPath.resolve(wrapperClassName.replace('.', File.separatorChar) + ".class")
            if (Files.notExists(wrapperClassPath))
                null
            else {
                val classLoader = new GeneratedClassClassLoader(generatedClassesPath, parent)
                Class.forName(wrapperClassName, false, classLoader).asInstanceOf[Class[_ <: PuppetWrapper[AnyRef]]]
            }
        }) match {
            case clazz: Class[S with PuppetWrapper[S]] => Some(clazz)
            case _                                     => None
        }
    }

    def compileQueue(parent: ClassLoader): Unit = {
        val sources = listSources()
                .map(pathToGeneratedClassName(_, 5))
        if (sources.isEmpty)
            return

        AppLogger.debug(s"Compiling ${sources.length} dynamic wrapper classes (${sources.mkString(", ")})...")

        val t0         = System.currentTimeMillis()
        val classPaths = ClassMappings.getClassPaths.map(source => Path.of(source.getLocation.toURI))
        compilersAccess
                .map(_.compileAll(queuePath, generatedClassesPath, classPaths))
                .find(_ != 0)
                .fold() {
                    throw new InvalidPuppetDefException("Some Compilers could not compile given source files. See above errors for more details.")
                }
        val t1         = System.currentTimeMillis()
        AppLogger.debug(s"Compilation took ${t1 - t0}ms.")

        sources.foreach(loadWrapperClass(_, parent))

        clearQueue()
    }

    private def loadWrapperClass(name: String, parent: ClassLoader): Unit = {
        val className = toWrapperClassName(name)
        val clazz     = new GeneratedClassClassLoader(generatedClassesPath, parent).loadClass(className)
        generatedClasses.put(className, clazz.asInstanceOf[Class[_ <: PuppetWrapper[AnyRef]]])
    }

    /*private def toWrappedClassName(puppetWrapperName: String): String = {
        val className  = puppetWrapperName.replace(File.separator, ".")
        val pivotIndex = className.lastIndexOf('.')

        var simpleName = className.drop(pivotIndex + 1)
        if (!simpleName.startsWith(WrapperPrefixName))
            return className
        simpleName = simpleName.drop(WrapperPrefixName.length)

        var packageName = className.take(pivotIndex)
        if (!packageName.startsWith(WrapperPackage))
            return packageName + simpleName
        packageName = packageName.drop(WrapperPackage.length)

        packageName + '.' + simpleName
    }*/

    private def toWrapperClassName(puppetWrapperName: String): String = {
        val className  = puppetWrapperName.replace(File.separator, ".")
        val pivotIndex = className.lastIndexOf('.')

        var simpleName = className.drop(pivotIndex + 1)
        if (simpleName.startsWith(WrapperPrefixName))
            return className
        simpleName = WrapperPrefixName + simpleName

        var packageName = className.take(pivotIndex)
        if (packageName.startsWith(WrapperPackage))
            return packageName + "." + simpleName
        packageName = WrapperPackage + packageName

        packageName + "." + simpleName
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
        Files.createDirectories(queuePath)
        Files.createDirectories(generatedClassesPath)

        clearQueue()
    }

    private def pathToGeneratedClassName(generatedClassPath: String, suffixLength: Int): String = {
        val parent = if (generatedClassPath.startsWith(generatedClassesPath.toString)) generatedClassesPath else queuePath
        val name   = generatedClassPath.drop(parent.toString.length + 1)
        WrapperPackage + name.replace(File.separator, ".").dropRight(suffixLength)
    }

    override def close(): Unit = {
        clearQueue()
    }

    private def clearQueue(): Unit = {
        //return

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
    val WrapperPackageName: String = "gen"
    val WrapperPackage    : String = WrapperPackageName + "."

    override def apply(resource: ResourceFolder): WrappersClassResource = new WrappersClassResource(resource)
}
