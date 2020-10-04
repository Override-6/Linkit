package fr.overridescala.vps.ftp.api

import java.io.Closeable

import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.api.task.{Task, TaskAction, TaskCompleterHandler, TaskExecutor}

/**
 * <p>
 * A Relay is the main interface implemented by the Server and Clients.
 * Tasks (link [[Task]]) gives you the possibility to control other computers, such as upload or downloading files/ folder,
 * creating Folders, retrieving some information about files etc...
 * </p>
 * <p>
 * Each Relay have a unique identifier, if a client try to connect with the same identifier of another connected client
 * his connection will be refused.
 * the RelayServer's identifier is forced to be "server". So, a client can't own this id
 * </p>
 * <p>
 * To create and execute tasks, a Relay have to be started. Then a [[TaskAction]] will be returned by Relay#scheduleTask([[TaskConcoctor]])
 * </p>
 *
 * @see [[TaskConcoctor]]
 * @see [[TaskAction]]
 * @see [[Closeable]]
 * @see [[Task]]
 * */
trait Relay extends Closeable {

    /**
     * The identifiers are required far task execution; they will be performed between you and the targeted Relay's identifier
     * two Relay can't have the same identifier in the network.
     * */
    val identifier: String

    /**
     * schedules a TaskExecutor.
     *
     * @param executor the task to schedule
     * @return a [[RelayTaskAction]] instance, this object allows you to enqueue or complete the task later.
     * @see [[RelayTaskAction]]
     * */
    def scheduleTask[R](task: Task[R]): RelayTaskAction[R]

    /**
     * @return the [[TaskCompleterHandler]] used by this Relay.
     * @see [[TaskCompleterHandler]]
     * */
    def getTaskCompleterHandler: TaskCompleterHandler

    /**
     * <b>Starts the Relay.</b>
     * <p>
     * This action is not automatically done, but is required to start the program
     * @throws RelayInitialisationException for any init error
     * */
    def start(): Unit

    /**
     * RelayTaskAction is a wraps a [[TaskAction]] object.
     * this class avoid the user to specify the task identifier
     * @see [[TaskAction]]
     * */
    case class RelayTaskAction[T] (taskAction: TaskAction[T]) {
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
