package fr.overridescala.vps.ftp.api

import java.io.Closeable

import fr.overridescala.vps.ftp.api.task.{Task, TaskAction, TaskCompleterFactory, TaskConcoctor}
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription}

/**
 * <p>
 * A Relay is the main interface implemented by the Server and Clients.
 * It's only creates Tasks, but this is enough to use the whole program.
 * Tasks gives you the possibility to control other computers, such as upload or downloading files/ folder,
 * creating Folders, retrieving some information about files etc...
 * </p>
 * <p>
 * Each Relay have a unique identifier, if a client try to connect with the same identifier of another connected client
 * his connection will be refused.
 * the RelayServer's identifier is forced to be "server". So, a client can't own this id
 * </p>
 * <p>
 * To create and execute tasks, a Relay have to be started.
 * </p>
 *
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
     * schedules a Task.
     *
     * @param concoctor the task to schedules
     * @return a [[TaskAction]] instance, this object allows you to enqueue or complete the task later.
     * */
    def scheduleTask[R, T >: TaskAction[R]](concoctor: TaskConcoctor[R, TaskAction[R]]): TaskAction[R]


    def getCompleterFactory: TaskCompleterFactory

    /**
     * <b>Starts the Relay.</b>
     * <p>
     * This action is not automatically done, but is required to start the program
     * */
    def start(): Unit

}
