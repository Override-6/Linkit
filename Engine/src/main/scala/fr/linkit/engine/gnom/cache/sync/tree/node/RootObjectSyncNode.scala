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

class RootObjectSyncNode[A <: AnyRef](data: ObjectNodeData[A]) extends ObjectSyncNode[A](null, data) {

    private var isPresentOnNetwork = false

    override def isPresentOnEngine(engineID: String): Boolean = isPresentOnNetwork //Root nodes are always synchronised between engines if they are present on the network.

    def setPresentOnNetwork(): Unit = {
        isPresentOnNetwork = true
    }

}
