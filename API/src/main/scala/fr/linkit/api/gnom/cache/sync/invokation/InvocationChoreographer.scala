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

package fr.linkit.api.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject

import scala.collection.mutable

class InvocationChoreographer {

    /**
     * All threads that figures in this set are running the
     * [[forceLocalInvocation]] method.
     * @see [[forceLocalInvocation]] for more details
     * */
    protected val markedThreads = new mutable.HashSet[Thread]

    /**
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
    def forceLocalInvocation[A](action: => A): A = {
        val thread = Thread.currentThread()
        markedThreads += thread
        try {
            action
        } finally {
            markedThreads -= thread
        }
    }

    /**
     * @return true if the current thread is marked as a thread that must perform all SynchronizedObject's executions locally.
     *         @see [[forceLocalInvocation()]]
     * */
    def isMethodExecutionForcedToLocal: Boolean = {
        val thread = Thread.currentThread()
        markedThreads.contains(thread) || InvocationChoreographer.markedThreads.contains(thread)
    }

}

object InvocationChoreographer extends InvocationChoreographer {
    override def isMethodExecutionForcedToLocal: Boolean = {
        markedThreads.contains(Thread.currentThread())
    }
}
