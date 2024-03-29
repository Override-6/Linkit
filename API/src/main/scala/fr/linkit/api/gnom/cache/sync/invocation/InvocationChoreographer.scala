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

package fr.linkit.api.gnom.cache.sync.invocation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject

import scala.collection.mutable

class InvocationChoreographer(parent: InvocationChoreographer) {

    def this() = this(InvocationChoreographer)

    /**
     * All threads that figures in this set are running the
     * [[disinv]] method.
     * @see [[disinv]] for more details
     * */
    protected val markedThreads = new mutable.HashSet[Thread]

    /**
     * Disable Sub Invocations<br>
     * The provided action will be executed, and during its execution,
     * the current thread will be added into the [[markedThreads]] set.
     *
     * A marked thread is a thread that will force all [[SynchronizedObject]] method's invocations
     * to execute the original method (simply make a super.xxx for a xxx generated method, instead of invoking the remote method.)
     *
     * @param action the action to perform
     * @tparam A return type of action
     *@return the return value of action
     * @see [[MethodInvocation]]
     * @see [[SynchronizedObject]] for more information about those 'generated methods'.
     * */
    def disinv[A](action: => A): A = {
        val thread = Thread.currentThread()
        if (markedThreads.contains(thread)) return action

        markedThreads += thread
        try {
            action
        } finally {
            markedThreads -= thread
        }
    }

    /**
     * Enable Sub Invocations<b>
     * */
    def ensinv[A](action: => A): A = {
        val thread = Thread.currentThread()
        if (!markedThreads.contains(thread)) return action

        markedThreads -= thread
        try {
            action
        } finally {
            markedThreads += thread
        }
    }

    /**
     * @return true if the current thread is marked as a thread that must perform all SynchronizedObject's executions locally.
     *         @see [[disinv()]]
     * */
    def isMethodExecutionForcedToLocal: Boolean = {
        markedThreads.contains(Thread.currentThread()) || parent.isMethodExecutionForcedToLocal
    }

}

object InvocationChoreographer extends InvocationChoreographer {

    override def isMethodExecutionForcedToLocal: Boolean = {
        markedThreads.contains(Thread.currentThread())
    }
}
