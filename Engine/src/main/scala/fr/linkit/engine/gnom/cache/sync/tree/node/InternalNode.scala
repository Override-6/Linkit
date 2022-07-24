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
import fr.linkit.api.gnom.cache.sync.tree.{ChippedObjectNode, ConnectedObjectNode, ObjectSyncNode}

sealed trait InternalNode[A <: AnyRef] extends MutableNode[A] with TrafficInterestedNode[A] with ConnectedObjectNode[A] {

}

trait InternalChippedObjectNode[A <: AnyRef] extends InternalNode[A] with ChippedObjectNode[A] {
    val chip: Chip[A]
}

trait InternalObjectSyncNode[A <: AnyRef] extends InternalChippedObjectNode[A] with ObjectSyncNode[A] with MutableSyncNode[A] {
    val puppeteer: Puppeteer[A]
}

