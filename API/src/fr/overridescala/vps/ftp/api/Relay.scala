package fr.overridescala.vps.ftp.api

import java.io.Closeable

import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.api.packet.PacketInterpreter
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.task.ext.TaskLoader
import fr.overridescala.vps.ftp.api.task.{Task, TaskAction}

//TODO reedit doc about all changes

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
 * @see [[TaskAction]]
 * @see [[Closeable]]
 * @see [[Task]]
 * */
trait Relay extends Closeable with TaskScheduler {

    val identifier: String

    /**
     * @return the [[PacketManager]] used by this relay.
     * @see [[PacketManager]]
     */
    val packetManager: PacketManager

    val taskLoader: TaskLoader

    val properties: RelayProperties

    val packetInterpreter: PacketInterpreter

    /**
     * <b>Starts the Relay.</b>
     * <p>
     * This action is not automatically done, but is required to start the program
     * @throws RelayInitialisationException for any init error
     * */
    def start(): Unit

}
