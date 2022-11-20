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

import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.env.{ChippedObjectCompanion, ConnectedObjectCompanion, SyncObjectCompanion}

sealed trait InternalCompanion[A <: AnyRef] extends TrafficInterestedCompanion[A] with ConnectedObjectCompanion[A] {

}

trait InternalChippedObjectCompanion[A <: AnyRef] extends InternalCompanion[A] with ChippedObjectCompanion[A] {
    val chip: Chip[A]
}

trait InternalSyncObjectCompanion[A <: AnyRef] extends InternalChippedObjectCompanion[A] with SyncObjectCompanion[A] with MutableSyncCompanion[A] {
    val puppeteer: Puppeteer[A]
}

