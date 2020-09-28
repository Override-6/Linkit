package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskInitPacket}

/**
 * The usable side for a [[TasksHandler]] to handle this task.
 * this trait can only execute (or pre-execute) tasks
 *
 * @see [[Task]]
 * @see [[TaskAction]]
 * */
trait TaskExecutor {

    /**
     * This method is invoked straight before [[execute]] but is only invoked if this task was created by the local Relay
     *
     * @return a TaskInitPacket that will be send in order to initialise target task's completer.
     * @see [[TaskInitPacket]]
     * */
    def newInitPacket(): TaskInitPacket = null

    /**
     * Executes this task.
     * @param channel the channel where the packet will be send and received
     *
     * @see [[PacketChannel]]
     * @see [[fr.overridescala.vps.ftp.api.packet.DataPacket]]
     * */
    def execute(channel: PacketChannel): Unit

}
