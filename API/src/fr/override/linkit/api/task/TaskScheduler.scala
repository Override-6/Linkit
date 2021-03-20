/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.task

trait TaskScheduler {


    /**
     * @return the [[TaskCompleterHandler]] used by this Relay.
     * @see [[TaskCompleterHandler]]
     * */
    val taskCompleterHandler: TaskCompleterHandler

    /**
     * schedules a TaskExecutor.
     *
     * @param task the task to schedule
     * @return a [[RelayTaskAction]] instance, this object allows you to enqueue or complete the task later.
     * @see [[RelayTaskAction]]
     * */
    def scheduleTask[R](task: Task[R]): RelayTaskAction[R]


    /**
     * RelayTaskAction is a wraps a [[TaskAction]] object.
     * this class avoid the user to specify the task identifier
     * @see [[TaskAction]]
     * */
    class RelayTaskAction[T](taskAction: TaskAction[T]) {
        def queue(onSuccess: T => Unit = null, onError: String => Unit = Console.err.println): Unit =
            taskAction.queue(onSuccess, onError)

        def complete(): T =
            taskAction.complete()
    }

    protected object RelayTaskAction {
        def of[T](taskAction: TaskAction[T]): RelayTaskAction[T] =
            new RelayTaskAction[T](taskAction)
    }

}
