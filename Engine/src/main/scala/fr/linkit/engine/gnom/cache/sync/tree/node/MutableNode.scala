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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.tree.ConnectedObjectNode
import org.jetbrains.annotations.Nullable

trait MutableNode[A <: AnyRef] extends ConnectedObjectNode[A] {

    def discoverParent(node: ObjectSyncNodeImpl[_]): Unit

    def addChild(child: MutableNode[_]): Unit

    def getChild[B <: AnyRef](id: Int): Option[MutableNode[B]]

}

trait MutableSyncNode[A <: AnyRef] extends MutableNode[A] {

    @Nullable
    def getMatchingSyncNode(origin: AnyRef): MutableSyncNode[_ <: AnyRef]
}