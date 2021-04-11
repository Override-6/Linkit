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

package fr.linkit.api.connection.task

trait TaskScheduler {

    /**
     * @return the [[TaskCompleterHandler]] used by this connection.
     * @see [[TaskCompleterHandler]]
     * */
    val taskCompleterHandler: TaskCompleterHandler

    /**
     * schedules a TaskExecutor.
     *
     * @param task the task to schedule
     * @return a [[InternalTaskAction]] instance, this object allows you to enqueue or complete the task later.
     * @see [[InternalTaskAction]]
     * */
    def scheduleTask[R](task: Task[R]): InternalTaskAction[R]

    /**
     * RelayTaskAction is a wraps a [[TaskAction]] object.
     * this class avoid the user to specify the task identifier
     * @see [[TaskAction]]
     * */
    class InternalTaskAction[T](taskAction: TaskAction[T]) {

        def queue(onSuccess: T => Unit = null, onError: String => Unit = Console.err.println): Unit =
            taskAction.queue(onSuccess, onError)

        def complete(): T =
            taskAction.complete()
    }

    protected object InternalTaskAction {

        def of[T](taskAction: TaskAction[T]): InternalTaskAction[T] =
            new InternalTaskAction[T](taskAction)
    }

}
