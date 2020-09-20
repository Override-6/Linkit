package fr.overridescala.vps.ftp.api.task

import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.packet.DataPacket

trait TasksHandler {

    def handlePacket(packet: DataPacket, factory: TaskCompleterFactory, ownerID: String, socket: SocketChannel): Unit

    def registerTask(executor: TaskExecutor, sessionID: Int, ownerID: String, ownFreeWill: Boolean)

}
