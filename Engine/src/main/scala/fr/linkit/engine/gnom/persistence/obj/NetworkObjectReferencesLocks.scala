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

package fr.linkit.engine.gnom.persistence.obj

import fr.linkit.api.gnom.referencing.NetworkObjectReference
import fr.linkit.engine.internal.concurrency.ReleasableReentrantLock

import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.collection.mutable

object NetworkObjectReferencesLocks {

    private final val lockMap = new mutable.WeakHashMap[NetworkObjectReference, Lock]
    private final val initLockMap = new mutable.HashMap[Int, ReleasableReentrantLock] //TODO try to make it weak

    /**
     * returns a reentrant lock associated with the given reference
     * @param reference the reference associated with it's reentrant lock.
     * */
    def getLock(reference: NetworkObjectReference): Lock = {
        lockMap.getOrElseUpdate(reference, new ReentrantLock)
    }

    private[linkit] def getInitializationLock(reference: NetworkObjectReference): ReleasableReentrantLock = {
        getInitializationLock(reference.hashCode())
    }

    private[linkit] def getInitializationLock(referenceHash: Int): ReleasableReentrantLock = {
        initLockMap.getOrElseUpdate(referenceHash, new ReleasableReentrantLock)
    }

}