package fr.overridescala.vps.ftp.server.connection

import java.io.Closeable
import java.net.Socket

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.task.{TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.server.task.ClientTasksHandler

class ClientConnectionThread(socket: Socket,
                             server: Relay) extends Thread with Closeable {

    private val out = socket.getInputStream
    private val packetLoader: PacketLoader = new PacketLoader

    val tasksHandler: TasksHandler = initialiseConnection()

    @volatile private var open = false

    override def run(): Unit = {
        while (open)
            update(tasksHandler.handlePacket)
    }

    def update(onPacketReceived: Packet => Unit): Unit = {
        //TODO predict packet length
        val bytes = out.readNBytes(Constants.MAX_PACKET_LENGTH)
        packetLoader.add(bytes)
        var packet: Packet = null
        packet = packetLoader.nextPacket
        while (packet != null) {
            onPacketReceived(packet)
            packet = packetLoader.nextPacket
        }
    }

    def close(): Unit = {
        tasksHandler.close()
        out.close()
        open = false
    }


    def initialiseConnection(): TasksHandler = {
        setName(s"RP Connection (unknownId)")
        val writer = SocketWriter.wrap(socket)
        val channel = new SimplePacketChannel(writer, Protocol.INIT_ID)
        channel.sendInitPacket(TaskInitInfo.of("GID", "nowhere"))

        deflectInChannel(channel)
        val identifier = channel.nextPacket().header

        channel.sendPacket("OK")
        setName(s"RP Connection ($identifier)")
        new ClientTasksHandler(identifier, server, writer)
    }


    private def deflectInChannel(channel: PacketChannelManager): Unit = {
        update {
            case data: DataPacket => channel.addPacket(data)
        }
    }

}
