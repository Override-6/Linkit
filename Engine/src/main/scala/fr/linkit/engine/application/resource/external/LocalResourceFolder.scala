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

package fr.linkit.engine.application.resource.external

import fr.linkit.api.application.resource.{OpenActionShortener, ResourceListener}
import fr.linkit.api.application.resource.external._
import fr.linkit.api.application.resource.representation.{ResourceRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.internal.system.fsa.FileAdapter
import fr.linkit.engine.application.resource.base.BaseResourceFolder

import scala.reflect.{ClassTag, classTag}

class LocalResourceFolder protected(adapter: FileAdapter,
                                    listener: ResourceListener,
                                    parent: ResourceFolder) extends BaseResourceFolder(parent, listener, adapter) with LocalFolder {

    //println(s"Creating resource folder $getLocation...")


    override def createOnDisk(): Unit = getAdapter.createAsFolder()

    override def scanFolders(scanAction: String => Unit): Unit = {
        scan(scanAction, true)
    }

    override def scanFiles(scanAction: String => Unit): Unit = {
        scan(scanAction, false)
    }

    private def scan(scanAction: String => Unit, filterDirs: Boolean): Unit = {
        fsa.list(getAdapter)
                .filter(_.isDirectory == filterDirs)
                .map(_.getName)
                .filterNot(maintainer.isKnown)
                .foreach(scanAction)
    }

}

object LocalResourceFolder extends ResourceFactory[LocalResourceFolder] {

    override def apply(adapter: FileAdapter,
                       listener: ResourceListener,
                       parent: ResourceFolder): LocalResourceFolder = {
        new LocalResourceFolder(adapter, listener, parent)
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
