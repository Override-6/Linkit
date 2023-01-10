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
import fr.linkit.api.gnom.cache.sync.contract.level.{ConcreteSyncLevel, MirrorableSyncLevel}
import fr.linkit.api.gnom.cache.sync.env.ObjectConnector
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.network.tag.Current
import fr.linkit.engine.gnom.cache.sync.{AbstractSynchronizedObject, IllegalSynchronizedObjectException}
import org.jetbrains.annotations.Nullable

class SyncObjectCompanionImpl[A <: AnyRef](data: SyncObjectCompanionData[A]) extends ChippedObjectCompanionImpl[A](data) with InternalSyncObjectCompanion[A] {

    override val puppeteer: Puppeteer[A] = data.puppeteer

    override def obj: A with SynchronizedObject[A] = data.obj

    override val isMirror   : Boolean = data.syncLevel == MirrorableSyncLevel.Mirror
    override val isOrigin   : Boolean = data.resolver.isEquivalent(Current, ownerTag)
    override val isMirroring: Boolean = isMirror && !isOrigin

    val connector: ObjectConnector = data.secondLayer

    initSyncObject()

    override def toString: String = s"node $reference for sync object ${obj.getClass.getName}"

    private def initSyncObject(): Unit = {
        obj match {
            case sync: AbstractSynchronizedObject[A] => sync.initialize(this)
            case _                                   => throw new IllegalSynchronizedObjectException(
                "Received unknown kind of synchronized object: " +
                        s"could not initialize synchronized object because received object does not implements ${classOf[AbstractSynchronizedObject[_]]}.")
        }
    }

}
