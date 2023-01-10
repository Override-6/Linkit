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

package linkit.base.resource.local

import fr.linkit.api.application.resource.local.{Resource, ResourceFactory, ResourceFile, ResourceFolder}

import java.nio.file.Files

object LocalResourceFactories {

    def adaptive: ResourceFactory[Resource] = (path, listener, parent) => {
        if (Files.isDirectory(path)) folder(path, listener, parent)
        else file(path, listener, parent)
    }

    def folder: ResourceFactory[ResourceFolder] = LocalResourceFolder.apply

    def file: ResourceFactory[ResourceFile] = (adapter, _, parent) => {
        LocalResourceFile(parent, adapter)
    }

}
