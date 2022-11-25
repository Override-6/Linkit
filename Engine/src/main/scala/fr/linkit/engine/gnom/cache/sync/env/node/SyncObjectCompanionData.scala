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

package fr.linkit.engine.gnom.cache.sync.env.node

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.level.{ConcreteSyncLevel, SyncLevel}
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer

import java.lang.ref.WeakReference

class SyncObjectCompanionData[A <: AnyRef](val puppeteer     : Puppeteer[A],
                                           synchronizedObject: A with SynchronizedObject[A],
                                           val syncLevel     : SyncLevel)
                                          (private val data: ChippedObjectCompanionData[A])
        extends ChippedObjectCompanionData[A](data) {

    def this(other: SyncObjectCompanionData[A]) = {
        this(other.puppeteer, other.obj, other.syncLevel)(other.data)
    }

    override def obj: A with SynchronizedObject[A] = synchronizedObject

}