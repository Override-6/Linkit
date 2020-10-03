package fr.overridescala.vps.ftp.server.connection

import java.io.Closeable
import java.net.Socket

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketLoader, SocketWriter}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.server.task.ClientTasksHandler

class ClientConnectionThread(socket: Socket,
                             val identifier: String,
                             server: Relay) extends Thread with Closeable {

    private val out = socket.getInputStream
    private val packetLoader: PacketLoader = new PacketLoader
    private val tasksHandler = new ClientTasksHandler(identifier, server, SocketWriter.wrap(socket))

    @volatile private var open = false

    override def run(): Unit = {
        open = true
        while (open)
            update()
    }

    def update(): Unit = {
        val bytes = out.readNBytes(Constants.MAX_PACKET_LENGTH)
        packetLoader.add(bytes)
        var packet: Packet = null
        packet = packetLoader.nextPacket
        while (packet != null) {
            tasksHandler.handlePacket(packet)
            packet = packetLoader.nextPacket
        }

    }

    def close(): Unit = {
        open = false
        tasksHandler.clearTasks()
    }

    setName(s"RP Connection ($identifier)")

}
