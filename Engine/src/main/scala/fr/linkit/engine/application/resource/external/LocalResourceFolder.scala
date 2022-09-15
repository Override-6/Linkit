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

package fr.linkit.engine.application.resource.external

import fr.linkit.api.application.resource.external._
import fr.linkit.api.application.resource.representation.{ResourceRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.application.resource.{OpenActionShortener, ResourceListener}
import fr.linkit.engine.application.resource.base.BaseResourceFolder
import org.jetbrains.annotations.Nullable

import java.nio.file.{Files, Path}
import scala.reflect.{ClassTag, classTag}

class LocalResourceFolder protected(adapter: Path,
                                    listener: ResourceListener,
                                    @Nullable parent: ResourceFolder) extends BaseResourceFolder(parent, listener, adapter) with LocalFolder {


    override def createOnDisk(): Unit = Files.createDirectories(getPath)

    override def scanFolders(scanAction: String => Unit): Unit = {
        scan(scanAction, true)
    }

    override def scanFiles(scanAction: String => Unit): Unit = {
        scan(scanAction, false)
    }

    private def scan(scanAction: String => Unit, filterDirs: Boolean): Unit = {
        Files.list(getPath)
                .toArray(new Array[Path](_))
                .filter(p => Files.isDirectory(p) == filterDirs)
                .map(_.getFileName.toString)
                .filterNot(maintainer.isKnown)
                .foreach(scanAction)
    }

}

object LocalResourceFolder extends ResourceFactory[LocalResourceFolder] {

    override def apply(path: Path,
                       listener: ResourceListener,
                       parent: ResourceFolder): LocalResourceFolder = {
        new LocalResourceFolder(path, listener, parent)
    }

    implicit def shortenRepresentation[E <: Resource : ClassTag, R <: ResourceRepresentation : ClassTag](name: String)
                                                                                                        (implicit resourceFactory: ResourceFactory[E],
                                                                                                         representationFactory: ResourceRepresentationFactory[R, E]): OpenActionShortener[R] = {
        { folder =>
            val resource = folder.getOrOpen[E](name)
            val entry    = resource.getEntry.asInstanceOf[ResourceEntry[E]]
            entry
                    .findRepresentation[R]()
                    .getOrElse {
                        entry.attachRepresentation[R]()(classTag[R], representationFactory)
                        entry.getRepresentation[R]()
                    }
        }
    }

    val ForbiddenChars: Array[Char] = Array(':', '?', '"', '<', '>', '|')

}
