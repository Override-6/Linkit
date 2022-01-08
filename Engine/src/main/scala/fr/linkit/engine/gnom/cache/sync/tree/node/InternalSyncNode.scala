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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.{ObjectSyncNode, SyncNode}

sealed trait InternalSyncNode[A <: AnyRef] extends MutableSyncNode[A] with TrafficInterestedSyncNode[A] with SyncNode[A] {

}

trait InternalObjectSyncNode[A <: AnyRef] extends InternalSyncNode[A] with ObjectSyncNode[A] {
    val puppeteer: Puppeteer[A]

    val chip: Chip[A]
}
