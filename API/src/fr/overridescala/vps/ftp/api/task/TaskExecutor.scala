package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.PacketChannel

/**
 * The usable side for a [[TasksHandler]] to handle this task.
 * this trait can only execute (or pre-execute) tasks
 *
 * @see [[Task]]
 * @see [[TaskAction]]
 * */
trait TaskExecutor {



    /**
     * This method is invoked straight before [[execute]] but is only invoked if this task was created by the Local Relay
     * Content of this method must only send packets to initialise a potential TaskCompleter (more information about how tasks are
     * created / enqueued from [[fr.overridescala.vps.ftp.api.packet.DataPacket]] in [[TasksHandler]] scala doc)
     * @param channel the channel where the packet will be send and received
     *
     * @see [[PacketChannel]]
     * @see [[PacketChannel]]
     * @see [[fr.overridescala.vps.ftp.api.packet.DataPacket]]
     * */
    def sendTaskInfo(channel: PacketChannel): Unit = {}

    /**
     * Executes this task.
     * @param channel the channel where the packet will be send and received
     *
     * @see [[PacketChannel]]
     * @see [[fr.overridescala.vps.ftp.api.packet.DataPacket]]
     * */
    def execute(channel: PacketChannel): Unit

}
