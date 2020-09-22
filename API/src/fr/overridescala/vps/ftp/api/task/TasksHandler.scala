package fr.overridescala.vps.ftp.api.task

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.DataPacket

/**
 * This class is the hearth of this program.
 * Tasks are registered and are enqueued. Then executed one behind the others
 *
 * <b><u>How tasks are created from packets :</u></b>
 *      First, all packets received are handled by the [[fr.overridescala.vps.ftp.api.Relay]], then by this TasksHandler.
 *      DataPackets have an identifier, this identifier is the identifier from which Task this packet is concerned.
 *      If the packet identifier differs from the current Task,
 *      this means that the Relay received a new Task request to enqueue and be executed after the current task was end
 *      DataPackets contains a identifier, a header and som content bytes
 *      The TaskCompleter
 *      The header of a init packet must be the Task name to create and enqueue.
 *
 * */
trait TasksHandler {

    def handlePacket(packet: DataPacket, ownerID: String, socket: SocketChannel): Unit

    def registerTask(executor: TaskExecutor, sessionID: Int, ownerID: String, ownFreeWill: Boolean)

    def getTaskCompleterFactory: TaskCompleterFactory
}
