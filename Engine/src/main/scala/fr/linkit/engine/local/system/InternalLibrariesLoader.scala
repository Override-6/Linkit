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

package fr.linkit.engine.local.system

import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.system.AppInitialisationException
import fr.linkit.engine.local.LinkitApplication

import java.io.File
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.zip.{ZipEntry, ZipFile}

object InternalLibrariesLoader {

    private val ResourceMark    = "/mapEngineFilter.txt"
    private val LibDestination = "system.internal.libraries_dir"
    private val InternalDestination = "system.internal_dir"

    def extractAndLoad(resources: ResourceFolder, libs: Array[String]): Unit = {
        val fileUrl        = classOf[LinkitApplication].getResource(ResourceMark)
        val path           = fileUrl.getPath
        val zipMarkerIndex = path.lastIndexOf("!")
        if (zipMarkerIndex > 0) {
            extractJar(resources, path.take(zipMarkerIndex), libs)
            return
        }
        val dirPath = path.dropRight(ResourceMark.length).drop(1)
        if (Files.isDirectory(Path.of(dirPath))) {
            extractFolder(resources, dirPath, libs)
        } else {
            throw AppInitialisationException("Linkit seems not to be run in a normal environment : Linkit's internal resources destination is not contained in a archive (jar, zip, tar, rar) or a folder.")
        }

    }

    private def extractFolder(resources: ResourceFolder, filePath: String, libs: Array[String]): Unit = {
        val root               = Path.of(filePath + "/natives/")
        val extractDestination = getPathProperty(resources, LibDestination)
        Files.list(root)
                .filter(p => libs.contains(p.getFileName.toString))
                .forEach { libPath =>
                    val destination = Path.of(extractDestination + "/" + libPath.getFileName)
                    Files.copy(libPath, destination)
                }
    }

    private def extractJar(resources: ResourceFolder, jarPath: String, libs: Array[String]): Unit = {
        val zipFile = new ZipFile(new File(jarPath))
        val root    = getPathProperty(resources, LibDestination)
        Files.createDirectories(root)
        libs.foreach { lib =>
            val entry = zipFile.getEntry(lib)
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
                if (entry.isDirectory) {
                    exportDirEntry(file, entry, path)
                } else {
                    val in = file.getInputStream(dirZip)
                    Files.write(path, in.readAllBytes(), StandardOpenOption.CREATE)
                }
            }
        }
    }

    private def getPathProperty(resources: ResourceFolder, name: String): Path = {
        Path.of(resources.getAdapter.getPath + '/' + LinkitApplication.getProperty(name))
    }

}
