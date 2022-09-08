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

import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.collection.mutable

object NetworkObjectReferencesLocks {

    private final val lockMap = new mutable.WeakHashMap[NetworkObjectReference, LockHolder]

    private def getLockHolder(reference: NetworkObjectReference): LockHolder = {
        lockMap.getOrElseUpdate(reference, new LockHolder())
    }

    /**
     * returns a reentrant lock associated with the given reference
     * @param reference the reference associated with it's reentrant lock.
     * */
    def getLock(reference: NetworkObjectReference): ReentrantLock = getLockHolder(reference).lock

    def getCondition(reference: NetworkObjectReference, tag: Int): Condition = {
        getLockHolder(reference).getCondition(tag)
    }

    private class LockHolder {

        val lock = new ReentrantLock()

        private val conditions = new mutable.WeakHashMap[Int, Condition]

        def getCondition(tag: Int): Condition = conditions.getOrElseUpdate(tag, lock.newCondition())
    }

}