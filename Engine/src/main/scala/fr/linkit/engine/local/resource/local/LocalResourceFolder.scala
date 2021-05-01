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

package fr.linkit.engine.local.resource.local

import fr.linkit.api.local.resource.ResourceListener
import fr.linkit.api.local.resource.external._
import fr.linkit.api.local.system.fsa.FileAdapter
import fr.linkit.engine.local.resource.base.BaseResourceFolder

class LocalResourceFolder protected(adapter: FileAdapter,
                                    listener: ResourceListener,
                                    parent: ResourceFolder) extends BaseResourceFolder(parent, listener, adapter) with LocalExternalFolder {

    //println(s"Creating resource folder $getLocation...")


    override def createOnDisk(): Unit = getAdapter.createAsFolder()

    override def scan(scanAction: String => Unit): Unit = {
        fsa.list(getAdapter)
                .map(_.getName)
                .filterNot(maintainer.isKnown)
                .foreach(scanAction)
    }

}

object LocalResourceFolder extends ExternalResourceFactory[LocalResourceFolder] {

    override def apply(adapter: FileAdapter,
                       listener: ResourceListener,
                       parent: ResourceFolder): LocalResourceFolder = {
        new LocalResourceFolder(adapter, listener, parent)
    }

    val ForbiddenChars: Array[Char] = Array('\\', '/', ':', '?', '"', '<', '>', '|')

}
