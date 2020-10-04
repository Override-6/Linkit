package fr.overridescala.vps.ftp.server.connection

import java.io.Closeable
import java.net.Socket

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.task.{TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.server.task.ClientTasksHandler

class ClientConnectionThread(socket: Socket,
                             server: Relay,
                             manager: ConnectionsManager) extends Thread with Closeable {

    private val packetReader: PacketReader = new PacketReader(socket)

    val tasksHandler: TasksHandler = initialiseConnection()
    val identifier: String = tasksHandler.identifier //shortcut
    @volatile private var open = false

    override def run(): Unit = {
        open = true
        while (open)
            update(tasksHandler.handlePacket)
    }

    override def close(): Unit = {
        tasksHandler.close()
        socket.close()
        open = false
    }

    private def update(onPacketReceived: Packet => Unit): Unit = {
        onPacketReceived(packetReader.readPacket())
    }


    private def initialiseConnection(): TasksHandler = {
        setName(s"RP Connection (unknownId)")
        val channel = new SimplePacketChannel(socket, Protocol.INIT_ID)
        channel.sendInitPacket(TaskInitInfo.of("GID", "nowhere"))

        deflectInChannel(channel)
        val identifier = channel.nextPacket().header
        val response = if (manager.containsIdentifier(identifier)) "ERROR" else "OK"
        channel.sendPacket(response)

        if (response.equals("ERROR")) {
            println("a Relay point connection have been rejected.")
            return null
        }
        println(s"Relay Point connected with identifier '$identifier'")
        setName(s"RP Connection ($identifier)")
        new ClientTasksHandler(identifier, server, socket)
    }


    private def deflectInChannel(channel: PacketChannelManager): Unit = {
        update {
            case data: DataPacket => channel.addPacket(data)
        }
    }

}
