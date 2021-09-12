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

package fr.linkit.engine.local.resource.external

import fr.linkit.api.local.resource.external._
import fr.linkit.api.local.resource.representation.{ResourceRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.local.resource.{OpenActionShortener, ResourceListener}
import fr.linkit.engine.local.resource.base.BaseResourceFolder

import java.nio.file.{Files, Path}
import scala.reflect.{ClassTag, classTag}

class LocalResourceFolder protected(path: Path,
                                    listener: ResourceListener,
                                    parent: ResourceFolder) extends BaseResourceFolder(parent, listener, path) with LocalFolder {

    //println(s"Creating resource folder $getLocation...")


    override def createOnDisk(): Unit = Files.createDirectories(path)

    override def scanFolders(scanAction: String => Unit): Unit = {
        scan(scanAction, true)
    }

    override def scanFiles(scanAction: String => Unit): Unit = {
        scan(scanAction, false)
    }

    private def scan(scanAction: String => Unit, filterDirs: Boolean): Unit = {
        Files.list(path)
                .filter(Files.isDirectory(_) == filterDirs)
                .map(_.getFileName.toString)
                .filter(!maintainer.isKnown(_))
                .forEach(scanAction(_))
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
                    .findRepresentation[R]
                    .getOrElse {
                        entry.attachRepresentation[R](classTag[R], representationFactory)
                        entry.getRepresentation[R]
                    }
        }
    }

    val ForbiddenChars: Array[Char] = Array(':', '?', '"', '<', '>', '|')

}
