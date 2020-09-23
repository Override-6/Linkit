package fr.overridescala.vps.ftp.api.task

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.DataPacket

/**
 * This class is the hearth of this program.
 * Tasks are registered and are enqueued, then executed one after the others
 *
 * <b><u>How tasks are created from packets :</u></b>
 *      1. First, all received packets are handled by the [[fr.overridescala.vps.ftp.api.Relay]], then this TasksHandler.
 *      DataPackets have an identifier, this identifier is the identifier from which Task this packet is concerned.
 *      2. If the packet identifier differs from the current Task, it means the Relay received a new Task to schedule and enqueue
 *      DataPackets contains a identifier, a header and some content bytes
 *      3. The [[TaskCompleterHandler]] creates a TaskExecutor from the init packet header (which is the task name). Then registers the task.
 *
 *      <b>Notes:</b>
 *          if the packet identifier is equals to the current task identifier, the TasksHandler will add the packet to
 *          the used [[fr.overridescala.vps.ftp.api.packet.PacketChannel]]
 *
 * @see PacketChannel
 * */
trait TasksHandler {

    /**
     * Handles the packet.
     * @param packet packet to handle
     * @param ownerID the sender identifier
     * @param socket the sender socket
     * */
    def handlePacket(packet: DataPacket, ownerID: String, socket: SocketChannel): Unit

    /**
     * Registers a task
     * @param executor the task to execute
     * @param taskIdentifier the task identifier
     * @param ownerID the identifier of from who this task come from
     * @param ownFreeWill true if the task was created by the user, false if the task comes from other Relay
     * */
    def registerTask(executor: TaskExecutor, taskIdentifier: Int, ownerID: String, ownFreeWill: Boolean): Unit


    /**
     * @return the [[TaskCompleterHandler]]
     * @see [[TaskCompleterHandler]]
     * */
    def getTasksCompleterHandler: TaskCompleterHandler
}
