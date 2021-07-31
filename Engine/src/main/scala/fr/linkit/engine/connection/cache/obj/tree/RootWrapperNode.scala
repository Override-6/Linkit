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

package fr.linkit.engine.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.tree.SynchronizedObjectTree
import fr.linkit.api.connection.cache.obj.{Chip, Puppeteer}

class RootWrapperNode[A](puppeteer: Puppeteer[A], chip: Chip[A], tree: SynchronizedObjectTree[_], platformIdentifier: String, id: Int)
        extends WrapperNode[A](puppeteer, chip, tree, platformIdentifier, id, null) {

    private var isPresentOnNetwork = false

    override def isPresentOnEngine(engineID: String): Boolean = isPresentOnNetwork //Root nodes are always synchronised between engines if they are present on the network.

    def setPresentOnNetwork(): Unit = {
        isPresentOnNetwork = true
    }


}
