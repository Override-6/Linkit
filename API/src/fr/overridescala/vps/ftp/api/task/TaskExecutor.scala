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
     * Executes this task.
     * @param channel the channel where the packet will be send and received
     *
     * @see [[PacketChannel]]
     * @see [[fr.overridescala.vps.ftp.api.packet.DataPacket]]
     * */
    def execute(channel: PacketChannel): Unit

}
