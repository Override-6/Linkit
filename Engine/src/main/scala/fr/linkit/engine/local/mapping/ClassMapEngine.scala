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

package fr.linkit.engine.local.mapping

import fr.linkit.api.local.system.AppLogger
import org.jetbrains.annotations.Nullable

import java.io.InputStream
import java.nio.file.{Files, Path}
import java.security.CodeSource
import java.util.zip.ZipFile

object ClassMapEngine {

    val ZipFileExtensions: Array[String] = Array(".jar", ".zip", ".rar", ".jmod")

    val JModRelativeClassesDirectory = "classes/"

    val EmptyFilter = new MapEngineFilters(null)

    val DefaultFilter = new MapEngineFilters(getClass.getResourceAsStream("/mapEngineFilter.txt"))

    def mapAllSources(sources: (CodeSource, ClassLoader)*): Unit = {
        sources.foreach(pair => {
            val source      = pair._1
            val classLoader = pair._2
            val url         = source.getLocation
            val root        = Path.of(url.toURI)
            AppLogger.debug(s"Mapping source $root...")
            ClassMappings.addClassPath(source)

            mapDirectory(root.toString, classLoader, root, EmptyFilter)
        })
    }

    def mapAllSourcesOfClasses(classes: Seq[Class[_]]): Unit = {
        val sources = classes.map(cl => (cl.getProtectionDomain.getCodeSource, cl.getClassLoader)).distinct
        mapAllSources(sources: _*)
    }

    def mapJDK(): Unit = {
        val jdkRoot = System.getProperty("java.home")
        val path    = Path.of(jdkRoot)
        AppLogger.debug(s"Mapping JDK ($path)...")
        mapDir(path, getClass.getClassLoader, DefaultFilter)
        AppLogger.debug("JDK Mapping done, classes were mapped in packages : ")
        DefaultFilter.filters.foreach(filter => {
            AppLogger.debug(s"\t${filter.line}")
        })
    }

    def mapDir(directory: Path,
               classLoader: ClassLoader, filters: MapEngineFilters = DefaultFilter): Unit = {
        mapDirectory(directory.toString, classLoader, directory, filters)
    }

    def mapPrimitives(): Unit = {
        import java.lang
        Array(
            Integer.TYPE,
            lang.Byte.TYPE,
            lang.Short.TYPE,
            lang.Long.TYPE,
            lang.Double.TYPE,
            lang.Float.TYPE,
            lang.Boolean.TYPE,
            Character.TYPE
        ).foreach(ClassMappings.putClass)
    }

    private def isZipFile(path: Path): Boolean = {
        val pathLow = path.toString.toLowerCase
        ZipFileExtensions.exists(zipExtension => pathLow.endsWith(zipExtension))
    }

    private def mapDirectory(root: String, classLoader: ClassLoader,
                             directory: Path,
                             filters: MapEngineFilters): Unit = {
        if (isZipFile(directory)) {
            val pathString = directory.toString
            val isInJMod   = pathString.endsWith(".jmod")
            val jarFile    = new ZipFile(pathString)
            jarFile.entries()
                    .asIterator()
                    .forEachRemaining(entry => {
                        val entryName = entry.getName
                        val classPath = if (isInJMod) entryName.drop(JModRelativeClassesDirectory.length) else entryName
                        mapPath(0, classLoader, classPath, filters)
                    })
            jarFile.close()
            return
        }
        Files.list(directory).forEach(path => {
            if (Files.isDirectory(path)) {
                mapDirectory(root, classLoader, path, filters)
            } else if (isZipFile(path)) {
                mapDirectory(root, classLoader, path, filters)
            } else {
                mapPath(root.length + 1, classLoader, path.toString, filters)
            }
        })
    }

    private def mapPath(rootCutLength: Int, classLoader: ClassLoader, path: String, filters: MapEngineFilters): Unit = {
        if (path.endsWith(".class")) {
            val classPath = path.substring(rootCutLength)
            val className = classPath
                    .replace('\\', '.')
                    .replace('/', '.')
                    .dropRight(".class".length)
            if (filters.canMap(className)) {
                ClassMappings.putClass(className, classLoader)
            }
        }
    }

    class MapEngineFilters(@Nullable in: InputStream) {

        lazy val filters: Array[MapEngineFilter] = {
            new String(in.readAllBytes(), "ASCII")
                    .split('\n')
                    .filterNot(_.isBlank)
                    .filterNot(_.strip().startsWith("#"))
                    .map(_.takeWhile(!_.isControl))
                    .map(MapEngineFilter)
        }

        def canMap(className: String): Boolean = {
            if (in == null)
                return true
            //println(s"filters = ${filters.mkString("Array(", ", ", ")")}")
            filters.exists(_.isAuthorised(className))
        }

    }

    case class MapEngineFilter(line: String) {

        private val isGeneralized = line.endsWith(".*")
        private val packageName   = if (isGeneralized) line.dropRight(2) else line

        def isAuthorised(className: String): Boolean = {
            //println(s"className = ${className}")
            //println(s"packageName = ${packageName}")
            if (isGeneralized)
                className.startsWith(packageName)
            else {
                val folder = className.take(className.lastIndexOf("."))
                //println(s"folder = ${folder}")
                packageName == folder
            }
        }
    }

}
