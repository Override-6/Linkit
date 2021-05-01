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

package fr.linkit.engine.connection.task

import fr.linkit.api.connection.task.{Fallible, Task, TaskException, TasksHandler}
import org.jetbrains.annotations.Nullable

import java.util.concurrent.atomic.AtomicReference

//TODO reedit the DOC
/**
 * <p>
 *     Task is a abstract task, all Tasks (excepted Completers) have to extends this class to be considered as a
 *     self-executable task.
 * </p>
 * <p>
 *     TasksCompleters does not have specific Class or Trait to extends, they just have to extend the TaskExecutor.
 *     TasksCompleters are created by the [[SimpleCompleterHandler]], and are normally not instantiable from other classes.
 *     TasksCompleters, are the tasks which completes the self-executable tasks.
 *
 *      @example
 * in [[CreateFileTask]], the self-executable (the class that directly extends from [[SimpleTask]]) will ask to the targeted Relay
 * if it could creates a file located on the specified path.
 * The targeted Relay will instantiate / execute the Completer of [[CreateFileTask]], in which the file will be created.
 * </p>
 * <p>
 * This class is a member of [[TaskAction]] and [[SimpleTaskExecutor]].
 * [[TaskAction]] is a Trait given to the user. this class only have enqueue and complete methods.
 * [[SimpleTaskExecutor]] is a Trait used by [[TasksHandler]] which will invoke TaskExecutor#execute nor TaskExecutor#sendTaskInfo if this task instance
 * was created by the program (!TaskCompleters)
 * </p>
 * @param targetID the targeted / concerned Relay identifier
 * @tparam T the return type of this Task when successfully executed
 * @see [[TasksHandler]]
 * @see [[SimpleCompleterHandler]]
 * @see [[TaskAction]]
 * @see [[SimpleTaskExecutor]]
 * */

abstract class SimpleTask[T](val targetID: String) extends Task[T] with Fallible {

    @volatile private var handler: TasksHandler = _

    /**
     * Invoked when the task execution was successful.
     * parameter 'T' is the return Type for why this task hardly worked for
     * */
    @volatile
    @Nullable private var onSuccess: T => Unit      = _
    /**
     *  Invoked when the task execution was unsuccessful.
     *  The String is the error message.
     *  Prints the error by default.
     * */
    @volatile
    @Nullable private var onFail   : String => Unit = Console.err.println

    /**
     * initialises this task.
     * a task can't be executed if it was not initialised
     * */
    final def preInit(tasksHandler: TasksHandler): SimpleTask[T] = {
        this.handler = tasksHandler
        this
    }

    /**
     * Enqueue / register this task to the [[TasksHandler]]
     * @param onSuccess the action to perform when the task was successful
     * @param onFail the action to perform when the task was unsuccessful
     * @param identifier specifies the task identifier used for packet channels.
     * */
    final override def queue(onSuccess: T => Unit = onSuccess, onFail: String => Unit = onFail, identifier: Int): Unit = {
        checkInit()
        this.onSuccess = onSuccess
        this.onFail = onFail
        handler.schedule(this, identifier, targetID, true)
    }

    /**
     * Completes the task. That does not mean that this task is not enqueued.
     * The particularity of this method, is that it will wait until the task end.
     * If the task was unsuccessful, throw an error, return the result instead.
     *
     * @throws TaskException if the task was unsuccessful
     * @return the task result
     * */
    final override def complete(identifier: Int): T = {
        checkInit()

        handler.schedule(this, identifier, targetID, true)
        val atomicResult = new AtomicReference[T]()

        onSuccess = result => synchronized {
            atomicResult.set(result)
            notifyAll()
        }

        onFail = msg => synchronized {
            Console.err.println(msg)
            notifyAll()
        }

        synchronized {
            wait()
        }
        atomicResult.get()
    }

    private[task] def checkInit(): Unit =
        if (handler == null)
            throw new TaskException("Please init this task before schedule it")

    /**
     * Invoked by TaskExecutors to signal that this task was unsuccessful
     * */
    override def fail(msg: String): Unit = {
        if (onFail != null)
            onFail(msg)
    }

    /**
     * Invoked by TaskExecutors to signal that this task was successful
     * */
    protected def success(t: T): Unit = {
        if (onSuccess != null)
            onSuccess(t)
    }

}
