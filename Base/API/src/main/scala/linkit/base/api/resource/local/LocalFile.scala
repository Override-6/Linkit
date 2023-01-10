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

package linkit.base.api.resource.local

import linkit.base.api.resource.ResourceListener
import fr.linkit.api.internal.system.delegate.ImplementationDelegates

import java.nio.file.Path

trait LocalFile extends ResourceFile with LocalResource {
    
}

object LocalFile extends ResourceFactory[LocalFile] {
    
    private val factories = ImplementationDelegates.resourceFactories
    
    implicit def factory: ResourceFactory[LocalFile] = factories.localFileFactory
    
    override def apply(adapter: Path, listener: ResourceListener, parent: Option[ResourceFolder]): LocalFile = factory(adapter, listener, parent)
}
