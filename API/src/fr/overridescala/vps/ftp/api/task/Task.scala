package fr.overridescala.vps.ftp.api.task

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.task.tasks.CreateFileTask
import org.jetbrains.annotations.Nullable

/**
 * <p>
 *     Task is a abstract task, all Tasks (excepted Completers) have to extends this class to be considered as a
 *     self-executable task.
 * </p>
 * <p>
 *     TasksCompleters does not have specific Class or Trait to extends, they just have to extend the TaskExecutor.
 *     TasksCompleters are created by the [[TaskCompleterHandler]], and are normally not instantiable from other classes.
 *     TasksCompleters, are the tasks which completes the self-executable tasks.
 *     @example
 *          in [[CreateFileTask]], the self-executable (the class that directly extends from [[Task]]) will ask to the targeted Relay
 *          if he could creates a file located on the specified path.
 *          The targeted Relay will instantiate / execute the Completer of [[CreateFileTask]], in which the file will be created.
 * </p>
 * <p>
 *      This class is a member of [[TaskAction]] and [[TaskExecutor]].
 *      [[TaskAction]] is a Trait given to the user. this class only have enqueue and complete methods.
 *      [[TaskExecutor]] is a Trait used by [[TasksHandler]] which will invoke TaskExecutor#execute nor TaskExecutor#sendTaskInfo if this task instance
 *      was created by the program (!TaskCompleters)
 * </p>
 * @param handler the TasksHandler
 * @param targetID the targeted / concerned Relay identifier
 * @tparam T the return type of this Task when successfully executed
 *
 * @see [[TasksHandler]]
 * @see [[TaskCompleterHandler]]
 * @see [[TaskAction]]
 * @see [[TaskExecutor]]
 * */
abstract class Task[T](private val handler: TasksHandler,
                       private val targetID: String)
        extends TaskAction[T] with TaskExecutor {

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
    @Nullable private var onError: String => Unit = Console.err.println
    /**
     * The session identifier is different from the Relay identifiers.
     * this identifier is implanted to packets who emerges from this task.
     * and is used by [[TasksHandler]] to determine if a packet concern this Task or not.
     * */
    private val identifier = ThreadLocalRandom.current().nextInt()

    /**
     * Enqueue / register this task to the [[TasksHandler]]
     * @param onSuccess the action to perform when the task was successful
     * @param onError the action to perform when the task was unsuccessful
     * */
    final override def queue(onSuccess: T => Unit = _ => onSuccess, onError: String => Unit = onError): Unit = {
        this.onSuccess = onSuccess
        this.onError = onError
        handler.registerTask(this, identifier, true, targetID)
    }

    /**
     * Completes the task. That does not mean that this task is not enqueued.
     * The particularity of this method, is that it will wait until the task end.
     * If the task was unsuccessful, throw an error, return the result instead.
     *
     * @throws TaskException if the task was unsuccessful
     * @return the task result
     * */
    final override def complete(): T = {
        handler.registerTask(this, identifier, true, targetID)
        val atomicResult = new AtomicReference[T]()
        val onSuccess: T => Unit = result => synchronized {
            notify()
            atomicResult.set(result)
        }
        val onError: String => Unit = msg => synchronized {
            throw new TaskException(msg + "\n")
            notify()
        }
        this.onSuccess = onSuccess
        this.onError = onError
        synchronized {
            wait()
        }
        atomicResult.get()
    }

    /**
     * Invoked by TaskExecutors to signal that this task was unsuccessful
     * */
    protected def error(msg: String): Unit = {
        if (onError != null)
            onError(msg)
    }

    /**
     * Invoked by TaskExecutors to signal that this task was successful
     * */
    protected def success(t: T): Unit = {
        if (onSuccess != null)
            onSuccess(t)
    }

}
