package fr.overridescala.vps.ftp.server.connection

import java.io.{BufferedOutputStream, Closeable}
import java.net.{Socket, SocketException}

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.RelayException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, ErrorPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.server.task.ClientTasksHandler

import scala.util.control.NonFatal

class ClientConnectionThread(socket: Socket,
                             server: Relay,
                             manager: ConnectionsManager) extends Thread with Closeable {

    private val packetManager = server.getPacketManager
    private val packetReader: PacketReader = new PacketReader(socket, server.getPacketManager)
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
        writer.write(packetManager.toBytes(packet))
        writer.flush()
    }

    private def handlePacket(packet: Packet): Unit = {
        packet match {
            case identifiablePacket: IdentifiablePacket =>
                if (identifiablePacket.targetIdentifier != server.identifier)
                    manager.deflectPacket(identifiablePacket)
                else tasksHandler.handlePacket(packet)
            case errorPacket: ErrorPacket =>
                errorPacket.errorType match {
                    case ErrorPacket.ABORT_TASK =>
                        printErrorPacket(errorPacket)
                        tasksHandler.skipCurrent()
                    case _ => tasksHandler.handlePacket(packet)
                }
        }
    }

    private def printErrorPacket(packet: ErrorPacket): Unit = {
        val identifier = tasksHandler.identifier
        val errorType = packet.errorType
        Console.err.println(s"received error from relay '$identifier' of type '$errorType'")
        packet.printError()
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


    private def executeError(e: RelayException): Unit = {
        Console.err.println(e.getMessage)
        val cause = if (e.getCause != null) e.getCause.getMessage else ""
        val packet = ErrorPacket(ErrorPacket.ABORT_TASK, e.getMessage, cause)
        writer.write(packetManager.toBytes(packet))
        writer.flush()
    }


    private def initialiseConnection(): TasksHandler = {
        setName(s"RP Connection (unknownId)")
        val channel = new SimplePacketChannel(socket, "unknownId", server.identifier, packetManager, -2)
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
