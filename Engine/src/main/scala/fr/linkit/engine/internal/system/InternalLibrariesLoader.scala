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

package fr.linkit.engine.internal.system

import fr.linkit.api.application.resource.local.ResourceFolder
import fr.linkit.api.internal.system.AppInitialisationException

import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.{Files, Path, StandardCopyOption, StandardOpenOption}
import java.util.zip.{ZipEntry, ZipFile}

private[linkit] object InternalLibrariesLoader {

    private val ResourceMark    = "/mapEngineFilter.txt"
    private val LibsDestination = "system.internal.libraries_dir"

    def extractAndLoad(resources: ResourceFolder, libs: Array[String]): Unit = {
        extract(resources, libs)
        load(resources)
    }

    private def load(resources: ResourceFolder): Unit = {
        val libsPath = getPathProperty(resources, LibsDestination)
        Files.list(libsPath)
             .forEach(loadLib)
    }

    private def loadLib(path: Path): Unit = System.load(path.toAbsolutePath.toString)

    private def extract(resources: ResourceFolder, libs: Array[String]): Unit = {
        val fileUrl        = classOf[LinkitApplication].getResource(ResourceMark)
        val path           = {
            val s = fileUrl.toString
            s.drop(s.indexOf("file:") + "file:".length)
        } //Removes "file:\" char header
        val zipMarkerIndex = path.lastIndexOf("!")
        if (zipMarkerIndex > 0) {
            extractJar(resources, path.take(zipMarkerIndex), libs)
            return
        }
        val dirPath = path.dropRight(ResourceMark.length)
        if (Files.isDirectory(Path.of(dirPath))) {
            extractFolder(resources, dirPath, libs)
        } else {
            throw AppInitialisationException("Linkit seems not to be run in a normal environment : Linkit's internal resources destination is not contained in a archive (jar, zip, tar, rar) or a folder.")
        }
    }

    private def extractFolder(resources: ResourceFolder, filePath: String, libs: Array[String]): Unit = {
        val root               = Path.of(filePath)
        val extractDestination = getPathProperty(resources, LibsDestination)
        Files.createDirectories(extractDestination)
        Files.list(root)
             .filter(p => libs.exists(toNativeLibName(_) == p.getFileName.toString))
             .forEach { libPath =>
                 val destination = Path.of(extractDestination + "/" + libPath.getFileName)
                 if (Files.notExists(destination)) {
                     Files.createDirectories(extractDestination)
                 }
                 Files.copy(libPath, destination, StandardCopyOption.REPLACE_EXISTING)
             }
    }

    private def toNativeLibName(libName: String): String = System.mapLibraryName(libName).drop(3)

    private def extractJar(resources: ResourceFolder, jarPath: String, libs: Array[String]): Unit = {
        val zipFile = new ZipFile(jarPath)
        val root    = getPathProperty(resources, LibsDestination)
        Files.createDirectories(root)
        libs.foreach { lib =>
            val entry = zipFile.getEntry(toNativeLibName(lib))
            exportDirEntry(zipFile, entry, root)
        }
    }

    private def exportDirEntry(file: ZipFile, dirZip: ZipEntry, destination: Path): Unit = {
        val entries = file.entries()
        val dirName = dirZip.getName
        while (entries.hasMoreElements) {
            val entry     = entries.nextElement()
            val entryName = entry.getName
            if (entryName.startsWith(dirName)) {
                val path = Path.of(destination.toString + '/' + entryName)
                /*if (entry.isDirectory) {
                    exportDirEntry(file, entry, path)*/
                if (!entry.isDirectory) {
                    val bytes = file.getInputStream(entry).readAllBytes()
                    Files.createDirectories(path.getParent)
                    Files.write(path, bytes, StandardOpenOption.CREATE)
                }
            }
        }
    }

    private def getPathProperty(resources: ResourceFolder, name: String): Path = {
        Path.of(resources.getPath + "/" + LinkitApplication.getProperty(name))
    }

}
