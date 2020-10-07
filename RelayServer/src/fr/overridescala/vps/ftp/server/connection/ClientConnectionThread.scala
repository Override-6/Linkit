package fr.overridescala.vps.ftp.server.connection

import java.io.{BufferedOutputStream, Closeable}
import java.net.{Socket, SocketException}

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.{RelayException, UnexpectedPacketException}
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.task.{TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.server.task.ClientTasksHandler

import scala.util.control.NonFatal

class ClientConnectionThread(socket: Socket,
                             server: Relay,
                             manager: ConnectionsManager) extends Thread with Closeable {


    private val packetReader: PacketReader = new PacketReader(socket)
    private val writer = new BufferedOutputStream(socket.getOutputStream)

    val tasksHandler: TasksHandler = initialiseConnection()

    @volatile private var open = false

    override def run(): Unit = {
        open = true
        val identifier = tasksHandler.identifier
        println(s"Thread '$getName' was started")
        try {
            while (open)
                update(handlePacket)
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" => Console.err.println(s"client '$identifier' disconnected.")
            case NonFatal(e) => e.printStackTrace()
        }
        close()
    }

    override def close(): Unit = {
        val identifier = tasksHandler.identifier
        println(s"closing '$identifier' thread")
        tasksHandler.close()
        socket.close()
        packetReader.close()
        manager.unregister(socket.getRemoteSocketAddress)
        open = false
        println(s"closed '$identifier' thread.")
    }

    private[connection] def sendDeflectedPacket(packet: Packet): Unit = {
        writer.write(packet)
        writer.flush()
    }

    private def handlePacket(packet: Packet): Unit = {
        if (packet.taskID != Protocol.ERROR_ID) {
            if (packet.targetIdentifier != server.identifier)
                manager.deflectPacket(packet)
            else
                tasksHandler.handlePacket(packet)
            return
        }
        if (!packet.isInstanceOf[DataPacket])
            throw UnexpectedPacketException("received unexpected packet type in error report channel")
        val data = packet.asInstanceOf[DataPacket]
        val identifier = tasksHandler.identifier
        Console.err.println(s"received error from relay '$identifier' of type '${data.header}'")
        Console.err.println(new String(data.header))
        if (data.header == Protocol.ABORT_TASK)
            tasksHandler.skipCurrent()
    }

    private def executeError(e: RelayException): Unit = {
        val identifier = tasksHandler.identifier
        Console.err.println(e.getMessage)
        writer.write(DataPacket(Protocol.ERROR_ID, Protocol.ABORT_TASK, identifier, server.identifier))
        writer.flush()
    }


    private def update(onPacketReceived: Packet => Unit): Unit = {
        try {
            packetReader
                    .readPacket()
                    .ifPresent(p => onPacketReceived(p))
        } catch {
            case e: RelayException => executeError(e)
        }
    }


    private def initialiseConnection(): TasksHandler = {
        setName(s"RP Connection (unknownId)")
        val channel = new SimplePacketChannel(socket, "unknownId", server.identifier, Protocol.INIT_ID)
        channel.sendInitPacket(TaskInitInfo.of("GID", "unknownId"))

        deflectInChannel(channel)
        val identifier = channel.nextPacket().header
        val response = if (manager.containsIdentifier(identifier)) "ERROR" else "OK"
        channel.sendPacket(response)

        if (response.equals("ERROR"))
            throw new RelayException("a Relay point connection have been rejected.")

        println(s"Relay Point connected with identifier '$identifier'")
        setName(s"RP Connection ($identifier)")
        new ClientTasksHandler(identifier, server, socket)
    }


    private def deflectInChannel(channel: PacketChannelManager): Unit =
        update {
            case data: DataPacket => channel.addPacket(data)
            case init: TaskInitPacket => handlePacket(init)
        }

}
