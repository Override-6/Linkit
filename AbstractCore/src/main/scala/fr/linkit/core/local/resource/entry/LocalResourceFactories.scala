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

package fr.linkit.core.local.resource.entry

import fr.linkit.api.local.resource.external.{ExternalResource, ExternalResourceFactory, ResourceFile, ResourceFolder}
import fr.linkit.api.local.system.fsa.FileAdapter

object LocalResourceFactories {

    def adaptive: ExternalResourceFactory[ExternalResource] = (adapter: FileAdapter, parent: ExternalResource) => {
        if (adapter.isDirectory) folder(adapter, parent)
        else file(adapter, parent)
    }

    def folder: ExternalResourceFactory[ResourceFolder] = (adapter: FileAdapter, parent: ExternalResource) => {
        LocalResourceFolder(adapter, parent)
    }

    def file: ExternalResourceFactory[ResourceFile] = (adapter: FileAdapter, parent: ExternalResource) => {

    }

}
