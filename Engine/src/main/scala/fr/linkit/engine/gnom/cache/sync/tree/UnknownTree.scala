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

package fr.linkit.engine.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.SynchronizedObject

import scala.collection.mutable.ListBuffer

class UnknownTree(val id: Int) {

    private val objects = ListBuffer.empty[AnyRef with SynchronizedObject[_]]

    def addUninitializedSyncObject(sync: SynchronizedObject[_]): Unit = {
        if (sync.isInitialized)
            throw new IllegalArgumentException("Sync Object is initialized.")
        objects += sync
    }

    def foreach(f: SynchronizedObject[_] => Unit): Unit = objects.foreach(f)

}
