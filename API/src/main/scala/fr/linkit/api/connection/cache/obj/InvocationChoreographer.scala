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

package fr.linkit.api.connection.cache.obj

import scala.collection.mutable

class InvocationChoreographer {

    /**
     * All threads that figures in this set are running the
     * [[forceLocalInvocation()]] method.
     * @see [[forceLocalInvocation()]] for more details
     * */
    protected val markedThreads = new mutable.HashSet[Thread]

    /**
     * The provided action will be executed, and during its execution,
     * the current thread will be added into the [[markedThreads]] set.
     *
     * A marked thread is a thread that will force all [[fr.linkit.api.connection.cache.obj.PuppetWrapper]] method's invocations
     * to execute the non overridden method (simply make a super.xxx for a xxx generated method, instead of invoking the remote method.)
     *
     * @param action the action to perform
     * @see [[fr.linkit.api.connection.cache.obj.PuppetWrapper]] for more information about those 'generated methods'.
     * */
    def forceLocalInvocation[A](action: => A): A = {
        markedThreads += Thread.currentThread()
        try {
            action
        } finally {
            markedThreads -= Thread.currentThread()
        }
    }

    /**
     * @return true if the current thread is marked as a thread that must perform all PuppetWrapper's executions locally.
     *         @see [[forceLocalInvocation()]]
     * */
    def isMethodExecutionForcedToLocal: Boolean = {
        markedThreads.contains(Thread.currentThread()) || InvocationChoreographer.isMethodExecutionForcedToLocal
    }

}

object InvocationChoreographer extends InvocationChoreographer {
    override def isMethodExecutionForcedToLocal: Boolean = {
        markedThreads.contains(Thread.currentThread())
    }
}
