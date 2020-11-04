package fr.overridescala.vps.ftp.server.connection

import java.io.{BufferedOutputStream, Closeable}
import java.net.{Socket, SocketException}

import fr.overridescala.vps.ftp.api.exceptions.RelayException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, ErrorPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.packet.ext.{PacketManager, PacketUtils}
import fr.overridescala.vps.ftp.api.task.{TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.server.RelayServer
import fr.overridescala.vps.ftp.server.task.ConnectionTasksHandler

import scala.util.control.NonFatal

class ClientConnectionThread(socket: Socket,
                             server: RelayServer,
                             manager: ConnectionsManager) extends Thread with Closeable {


    private val packetManager = server.packetManager
    private val packetReader: PacketReader = new PacketReader(socket)
    private val writer = new BufferedOutputStream(socket.getOutputStream)
    private val channelCache = new PacketChannelManagerCache

    val tasksHandler: TasksHandler = initialiseConnection()
    val identifier: String = tasksHandler.identifier //shortcut

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

    private[server] def createSync(id: Int): SyncPacketChannel =
        new SyncPacketChannel(socket, identifier, server.identifier, id, channelCache, packetManager)

    private[server] def createAsync(id: Int): AsyncPacketChannel =
        new AsyncPacketChannel(identifier, server.identifier, id, channelCache, socket)

    private[connection] def sendDeflectedBytes(bytes: Array[Byte]): Unit = {
        writer.write(bytes.length.toString.getBytes ++ PacketManager.SizeSeparator ++ bytes)
        writer.flush()
    }

    private def handlePacket(packet: Packet): Unit = {
        packet match {
            case errorPacket: ErrorPacket if errorPacket.errorType == ErrorPacket.ABORT_TASK =>
                printErrorPacket(errorPacket)
                tasksHandler.skipCurrent()
            case init: TaskInitPacket => tasksHandler.handlePacket(init)
            case _: Packet => channelCache.injectPacket(packet)
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
            val bytes = packetReader.readNextPacketBytes()
            if (bytes == null)
                return

            val target = getTargetID(bytes)
            if (target == server.identifier) {
                onPacketReceived(packetManager.toPacket(bytes))
                return
            }
            manager.deflectTo(bytes, target)
        } catch {
            case e: RelayException => executeError(e)
        }
    }


    private def executeError(e: RelayException): Unit = {
        Console.err.println(e.getMessage)
        val cause = if (e.getCause != null) e.getCause.getMessage else ""
        val packet = new ErrorPacket(-1,
            server.identifier,
            tasksHandler.identifier,
            ErrorPacket.ABORT_TASK,
            e.getMessage,
            cause)
        writer.write(packetManager.toBytes(packet))
        writer.flush()
    }


    private def initialiseConnection(): TasksHandler = {
        setName(s"RP Connection (unknownId)")
        implicit val channel: SyncPacketChannel =
            new SyncPacketChannel(socket, "unknown", server.identifier, -6, channelCache, packetManager)
        channel.sendInitPacket(TaskInitInfo.of("GID", "unknownId"))

        deflectInChannel(channel)
        val clientResponse = channel.nextPacket()
        clientResponse match {
            case errorPacket: ErrorPacket =>
                errorPacket.printError()
                throw new RelayException("a Relay point connection have been aborted by the client.")
            case dataPacket: DataPacket =>
                val identifier = dataPacket.header
                val response = if (manager.containsIdentifier(identifier)) "ERROR" else "OK"
                channel.sendPacket(DataPacket(response))

                if (response == "ERROR")
                    throw new RelayException("a Relay point connection have been rejected.")

                println(s"Relay Point connected with identifier '$identifier'")
                setName(s"RP Connection ($identifier)")
                new ConnectionTasksHandler(identifier, server, socket)
        }

    }


    private def deflectInChannel(channel: PacketChannelManager): Unit =
        update {
            case init: TaskInitPacket => handlePacket(init)
            case other: Packet => channel.addPacket(other)
        }

    def getTargetID(bytes: Array[Byte]): String = {
        PacketUtils.cutString(PacketManager.SenderSeparator, PacketManager.TargetSeparator)(bytes)
    }

}
