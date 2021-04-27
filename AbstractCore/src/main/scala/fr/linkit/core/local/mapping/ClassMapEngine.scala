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

package fr.linkit.core.local.mapping

import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.local.system.fsa.{FileAdapter, FileSystemAdapter}
import org.jetbrains.annotations.Nullable

import java.io.InputStream
import java.security.CodeSource
import java.util.zip.ZipFile

object ClassMapEngine {

    val ZipFileExtensions: Array[String] = Array(".jar", ".zip", ".rar", ".jmod")

    val JModRelativeClassesDirectory = "classes/"

    val EmptyFilter   = new MapEngineFilters(null)
    val DefaultFilter = new MapEngineFilters(getClass.getResourceAsStream("/mapEngineFilter.txt"))

    def mapAllSources(fsa: FileSystemAdapter, sources: (CodeSource, ClassLoader)*): Unit = {
        sources.foreach((pair) => {
            val source      = pair._1
            val classLoader = pair._2
            val url         = source.getLocation
            val root        = fsa.getAdapter(url.toURI)
            val rootPath    = root.getAbsolutePath
            AppLogger.debug(s"Mapping source $rootPath...")
            ClassMappings.putSourceCode(source)

            mapDirectory(fsa, rootPath, root, classLoader, EmptyFilter)
        })
    }

    def mapAllSourcesOfClasses(fsa: FileSystemAdapter, classes: Seq[Class[_]]): Unit = {
        val sources = classes.map(cl => (cl.getProtectionDomain.getCodeSource, cl.getClassLoader)).distinct
        mapAllSources(fsa, sources: _*)
    }

    def mapJDK(fsa: FileSystemAdapter): Unit = {
        val jdkRoot = System.getProperty("java.home")
        val adapter = fsa.getAdapter(jdkRoot)
        mapDir(fsa, adapter, getClass.getClassLoader)
    }

    def mapDir(fsa: FileSystemAdapter, directory: FileAdapter,
               classLoader: ClassLoader, filters: MapEngineFilters = DefaultFilter): Unit = {
        mapDirectory(fsa, directory.getAbsolutePath, directory, classLoader, filters)
    }

    private def isZipFile(directory: FileAdapter): Boolean = {
        val path = directory.getAbsolutePath.toLowerCase
        ZipFileExtensions.exists(zipExtension => path.endsWith(zipExtension))
    }

    private def mapDirectory(fsa: FileSystemAdapter, root: String,
                             directory: FileAdapter, classLoader: ClassLoader,
                             filters: MapEngineFilters): Unit = {
        if (isZipFile(directory)) {
            val dirPath  = directory.getAbsolutePath
            val isInJMod = dirPath.endsWith(".jmod")
            val jarFile  = new ZipFile(dirPath)
            jarFile.entries()
                    .asIterator()
                    .forEachRemaining(entry => {
                        val entryName = entry.getName
                        val classPath = if (isInJMod) entryName.drop(JModRelativeClassesDirectory.length) else entryName
                        mapPath(0, classLoader, classPath, filters)
                    })
            //jarFile.close()
            return
        }
        fsa.list(directory).foreach(adapter => {
            if (adapter.isDirectory) {
                mapDirectory(fsa, root, adapter, classLoader, filters)
            } else if (isZipFile(adapter)) {
                mapDirectory(fsa, root, adapter, classLoader, filters)
            } else {
                mapPath(root.length + 1, classLoader, adapter.getAbsolutePath, filters)
            }
        })
    }

    private def mapPath(rootCutLength: Int, classLoader: ClassLoader, path: String, filters: MapEngineFilters): Unit = {
        if (path.endsWith(".class")) {
            val classPath = path.substring(rootCutLength)
            val className = classPath
                    .replace('\\', '.')
                    .replace('/', '.')
                    .replace(".class", "")
            if (filters.canMap(className)) {
                ClassMappings.putClass(className, classLoader)
            }
        }
    }

    class MapEngineFilters(@Nullable in: InputStream) {

        private lazy val filters = {
            new String(in.readAllBytes())
                    .split('\n')
                    .filterNot(_.trim.startsWith("#"))
                    .map(new MapEngineFilter(_))
        }

        def canMap(className: String): Boolean = {
            if (in == null)
                return true
            filters.exists(_.isAuthorised(className))
        }

    }

    class MapEngineFilter(line: String) {

        private val isGeneralized = line.endsWith(".*")
        private val packageName   = if (isGeneralized) line.dropRight(1) else line

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
