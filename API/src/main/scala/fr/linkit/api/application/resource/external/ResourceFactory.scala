/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.application.resource.external

import fr.linkit.api.application.resource.ResourceListener
import fr.linkit.api.internal.system.fsa.FileAdapter

trait ResourceFactory[E <: Resource] {

    implicit final val self: this.type = this

    def apply(adapter: FileAdapter, listener: ResourceListener, parent: ResourceFolder): E
}
