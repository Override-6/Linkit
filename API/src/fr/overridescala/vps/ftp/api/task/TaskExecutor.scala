package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, TaskInitPacket}
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
     * This method value is used straight before task [[execute]] and only if a task where enqueued by the local Relay
     * It determines the way to instantiate a completer for this executor.
     *
     * @return a [[TaskInitInfo]] that describe how the completer will be instantiated
     * @see [[TaskInitPacket]]
     * */
    def initInfo: TaskInitInfo = null

    /**
     * Executes this task.
     * @param channel the channel where the packet will be send and received
     *
     * @see [[PacketChannel]]
     * @see [[DataPacket]]
     * */
    def execute(channel: PacketChannel): Unit

}
