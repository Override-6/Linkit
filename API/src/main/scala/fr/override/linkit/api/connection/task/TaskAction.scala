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

package fr.`override`.linkit.api.connection.task

import java.util.concurrent.ThreadLocalRandom

/**
 * The usable side for a user to handle this Task.
 * this trait can only enqueue, or complete the task
 *
 * @see [[Task]]
 * @see [[TaskAction]]
 * */
trait TaskAction[T] {
    /**
     * The session identifier is different from the Relay identifiers.
     * this identifier is implanted to packets who emerges from this task.
     * and is used by [[TasksHandler]] to determine if a packet concern this Task or not.
     * */
    protected val identifier: Int = ThreadLocalRandom.current().nextInt()

    /**
     * Enqueue / register this task.
     * The task will be executed after all task before are ended.
     * @param onSuccess the action to perform when the task was successful
     * @param onFail the action to perform when the task was unsuccessful
     * @param identifier specifies the task identifier used for packet channels.
     * */
    def queue(onSuccess: T => Unit = () => _, onFail: String => Unit = () => _, identifier: Int = identifier): Unit

    /**
     * Completes the task. That does not mean that this task is not enqueued.
     * The particularity of this method, is that it will wait until the task end.
     * If the task was unsuccessful, throw an error, return the result instead.
     *
     * @return the task result
     * */
    def complete(identifier: Int = identifier): T

}
