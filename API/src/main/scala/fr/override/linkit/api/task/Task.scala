package fr.`override`.linkit.api.task

import java.util.concurrent.atomic.AtomicReference

import fr.`override`.linkit.api.exception.TaskException
import org.jetbrains.annotations.Nullable


//TODO reedit the DOC
/**
 * <p>
 *     Task is a abstract task, all Tasks (excepted Completers) have to extends this class to be considered as a
 *     self-executable task.
 * </p>
 * <p>
 *     TasksCompleters does not have specific Class or Trait to extends, they just have to extend the TaskExecutor.
 *     TasksCompleters are created by the [[TaskCompleterHandler]], and are normally not instantiable from other classes.
 *     TasksCompleters, are the tasks which completes the self-executable tasks.
 *      @example
 *          in [[CreateFileTask]], the self-executable (the class that directly extends from [[Task]]) will ask to the targeted Relay
 *          if it could creates a file located on the specified path.
 *          The targeted Relay will instantiate / execute the Completer of [[CreateFileTask]], in which the file will be created.
 * </p>
 * <p>
 *      This class is a member of [[TaskAction]] and [[TaskExecutor]].
 *      [[TaskAction]] is a Trait given to the user. this class only have enqueue and complete methods.
 *      [[TaskExecutor]] is a Trait used by [[TasksHandler]] which will invoke TaskExecutor#execute nor TaskExecutor#sendTaskInfo if this task instance
 *      was created by the program (!TaskCompleters)
 * </p>
 * @param targetID the targeted / concerned Relay identifier
 * @tparam T the return type of this Task when successfully executed
 *
 * @see [[TasksHandler]]
 * @see [[TaskCompleterHandler]]
 * @see [[TaskAction]]
 * @see [[TaskExecutor]]
 * */

abstract class Task[T](val targetID: String) extends TaskExecutor with TaskAction[T] {

    @volatile private var handler: TasksHandler = _

    /**
     * Invoked when the task execution was successful.
     * parameter 'T' is the return Type for why this task hardly worked for
     * */
    @volatile
    @Nullable private var onSuccess: T => Unit = _
    /**
     *  Invoked when the task execution was unsuccessful.
     *  The String is the error message.
     *  Prints the error by default.
     * */
    @volatile
    @Nullable private var onFail: String => Unit = Console.err.println

    /**
     * initialises this task.
     * a task can't be executed if it was not initialised
     * */
    final def preInit(tasksHandler: TasksHandler): Task[T] = {
        this.handler = tasksHandler
        this
    }

    /**
     * Enqueue / register this task to the [[TasksHandler]]
     * @param onSuccess the action to perform when the task was successful
     * @param onFail the action to perform when the task was unsuccessful
     * @param identifier specifies the task identifier used for packet channels.
     * */
    final override def queue(onSuccess: T => Unit = _ => onSuccess, onFail: String => Unit = onFail, identifier: Int): Unit = {
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

        onSuccess  = result => synchronized {
            atomicResult.set(result)
            notifyAll()
        }

        onFail = msg => synchronized {
            Console.err.println(msg)
            notifyAll()
        }

        synchronized {
            wait
        }
        atomicResult.get()
    }

    private[task] def checkInit(): Unit =
        if (handler == null)
            throw new TaskException("Please init this task before schedule it")

    /**
     * Invoked by TaskExecutors to signal that this task was unsuccessful
     * */
    protected def fail(msg: String): Unit = {
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
