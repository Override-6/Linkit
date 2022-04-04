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

package fr.linkit.engine.internal.mapping

import fr.linkit.api.internal.system.AppLogger
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
            val rootPath    = root.toString
            AppLogger.debug(s"Mapping source $rootPath...")
            ClassMappings.addClassPath(source)

            mapDirectory(rootPath, root, classLoader, EmptyFilter)
        })
    }

    def mapAllSourcesOfClasses(classes: Seq[Class[_]]): Unit = {
        val sources = classes.map(cl => (cl.getProtectionDomain.getCodeSource, cl.getClassLoader)).distinct
        mapAllSources(sources: _*)
    }

    def mapJDK(): Unit = {
        val jdkRoot = System.getProperty("java.home")
        val adapter = Path.of(jdkRoot)
        AppLogger.debug(s"Mapping JDK ($adapter)...")
        mapDir(adapter, getClass.getClassLoader, DefaultFilter)
        AppLogger.debug("JDK Mapping done, classes were mapped in packages : ")
        DefaultFilter.filters.foreach(filter => {
            AppLogger.debug(s"\t${filter.line}")
        })
    }

    def mapDir(directory: Path,
               classLoader: ClassLoader, filters: MapEngineFilters = DefaultFilter): Unit = {
        mapDirectory(directory.toString, directory, classLoader, filters)
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

    private def isZipFile(directory: Path): Boolean = {
        val path = directory.toString.toLowerCase
        ZipFileExtensions.exists(zipExtension => path.endsWith(zipExtension))
    }

    private def mapDirectory(root: String,
                             directory: Path, classLoader: ClassLoader,
                             filters: MapEngineFilters): Unit = {
        if (isZipFile(directory)) {
            val dirPath  = directory.toString
            val isInJMod = dirPath.endsWith(".jmod")
            val jarFile  = new ZipFile(dirPath)
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
                mapDirectory(root, path, classLoader, filters)
            } else if (isZipFile(path)) {
                mapDirectory(root, path, classLoader, filters)
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
                try {
                    ClassMappings.putClass(className, classLoader)
                } catch {
                    case e: NoClassDefFoundError =>
                        AppLogger.warn(s"$className is not a valid class")
                }
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
