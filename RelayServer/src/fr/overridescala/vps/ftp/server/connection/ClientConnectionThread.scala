package fr.overridescala.vps.ftp.server.connection

import java.io.Closeable
import java.net.{Socket, SocketException}

import fr.overridescala.vps.ftp.api.exceptions.RelayException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, EmptyPacket, ErrorPacket, SystemPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.packet.ext.{PacketManager, PacketUtils}
import fr.overridescala.vps.ftp.api.task.{TaskInitInfo, TasksHandler}
import fr.overridescala.vps.ftp.server.RelayServer
import fr.overridescala.vps.ftp.server.task.ConnectionTasksHandler

import scala.util.control.NonFatal

class ClientConnectionThread(socket: SocketContainer,
                             server: RelayServer,
                             manager: ConnectionsManager) extends Thread with Closeable {


    private val packetManager = server.packetManager
    private val packetReader: PacketReader = new PacketReader(socket)
    private val channelCache = new PacketChannelManagerCache

    val tasksHandler: TasksHandler = initialiseConnection()
    val identifier: String = tasksHandler.identifier //shortcut
    private implicit val systemChannel: PacketChannel.Sync = createSync(-2)

    @volatile private var closed = false
    @volatile private var connected = true

    override def run(): Unit = {
        if (closed)
            throw new RelayException("This Connection was already used and is now definitely closed")

        println(s"Thread '$getName' was started")
        try {
            while (!closed)
                update(handlePacket)
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
        println(s"End of Thread execution '$getName'")
    }

    override def close(): Unit = closeConnection(true)

    private[server] def createSync(id: Int): SyncPacketChannel =
        new SyncPacketChannel(socket, identifier, server.identifier, id, channelCache, packetManager)

    private[server] def createAsync(id: Int): AsyncPacketChannel =
        new AsyncPacketChannel(identifier, server.identifier, id, channelCache, socket)

    private[server] def updateSocket(socket: Socket): Unit =
        this.socket.set(socket)

    private[connection] def sendDeflectedBytes(bytes: Array[Byte]): Unit = {
        socket.write(bytes.length.toString.getBytes ++ PacketManager.SizeSeparator ++ bytes)
    }

    private def closeConnection(requestIsLocal: Boolean): Unit = {
        println(s"closing thread '$getName'")
        if (connected && socket.isConnected) {
            if (requestIsLocal) {
                systemChannel.sendPacket(SystemPacket(SystemPacket.ClientClose))
                systemChannel.nextPacket() //Wait a response packet (EmptyPacket) before closing the connection.
            } else systemChannel.sendPacket(EmptyPacket())
            systemChannel.close()
        }

        tasksHandler.close()
        socket.close()
        manager.unregister(socket.remoteSocketAddress())

        closed = true
        println(s"thread '$getName' closed")
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
            listenNextConcernedPacket(onPacketReceived)
        } catch {
            case e: RelayException => executeError(e)
            case e: SocketException if e.getMessage == "Connection reset" =>
                Console.err.println(s"client '$identifier' disconnected.")
                println("Starting reconnection in 5 seconds...")
                Thread.sleep(5000)

        }
    }

    private def listenNextConcernedPacket(event: Packet => Unit): Unit = {
        val bytes = packetReader.readNextPacketBytes()
        if (bytes == null)
            return

        val target = getTargetID(bytes)
        if (target == server.identifier) {
            val packet = packetManager.toPacket(bytes)
            packet match {
                case orderPacket: SystemPacket => handleSystemOrder(orderPacket.order)
                case _: Packet => event(packet)
            }
            return
        }
        manager.deflectTo(bytes, target)
    }

    private def handleSystemOrder(order: String): Unit = {
        println(s"order $order had been requested by '$identifier'")
        order match {
            case SystemPacket.ClientClose => closeConnection(false)
            case SystemPacket.ServerClose => server.close()
            case _ =>
                Console.err.println(s"Could not find action for order '$order'")
                return
        }
    }

    private def executeError(e: RelayException): Unit = {
        Console.err.println(e.getMessage)
        val cause = if (e.getCause != null) e.getCause.getMessage else ""
        val packet = ErrorPacket(-1,
            server.identifier,
            tasksHandler.identifier,
            ErrorPacket.ABORT_TASK,
            e.getMessage,
            cause)
        socket.write(packetManager.toBytes(packet))
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
                channel.close()
                throw new RelayException("a Relay point connection have been aborted by the client.")
            case dataPacket: DataPacket =>
                val identifier = dataPacket.header
                val response = if (manager.containsIdentifier(identifier)) "ERROR" else "OK"
                channel.sendPacket(DataPacket(response))
                channel.close()

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

    private def getTargetID(bytes: Array[Byte]): String =
        PacketUtils.cutString(PacketManager.SenderSeparator, PacketManager.TargetSeparator)(bytes)

}
