package fr.overridescala.vps.ftp.server.connection

import java.net.{Socket, SocketException}

import fr.overridescala.vps.ftp.api.exceptions.{RelayException, UnexpectedPacketException}
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.fundamental._
import fr.overridescala.vps.ftp.api.packet.ext.{PacketManager, PacketUtils}
import fr.overridescala.vps.ftp.api.task.TasksHandler
import fr.overridescala.vps.ftp.api.{Reason, RelayCloseable}
import fr.overridescala.vps.ftp.server.RelayServer
import fr.overridescala.vps.ftp.server.task.ConnectionTasksHandler

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ClientConnectionThread(socket: SocketContainer,
                             server: RelayServer,
                             manager: ConnectionsManager) extends Thread with RelayCloseable {
     private val packetManager = server.packetManager
    private val packetReader: PacketReader = new PacketReader(socket)
    private val notifier = server.eventDispatcher.notifier
    private val channelCache = new PacketChannelManagerCache(notifier)
    /**
     * This array is only used and updated while the connection is not initialised.
     * */
    private val unhandledPackets = ListBuffer.empty[Packet]

    val tasksHandler: TasksHandler = initialiseConnection()
    val identifier: String = tasksHandler.identifier //shortcut

    private implicit val systemChannel: PacketChannel.Sync = createSync(-6)

    @volatile private var closed = false

    override def run(): Unit = {
        if (closed)
            throw new RelayException("This Connection was already used and is now definitely closed")

        println(s"Thread '$getName' was started")

        unhandledPackets.foreach(handlePacket)
        unhandledPackets.clear()

        try {
            while (!closed)
                update(handlePacket)
        } catch {
            case e: RelayException =>
                e.printStackTrace()
                notifier.onSystemError(e)
            case NonFatal(e) => e.printStackTrace()
        }
        println(s"End of Thread execution '$getName'")
    }

    override def close(reason: Reason): Unit = {
        println(s"closing thread '$getName'")
        if (socket.isConnected) {
            if (reason != Reason.EXTERNAL_REQUEST) {
                systemChannel.sendPacket(SystemPacket(SystemPacket.ClientClose))
            } else {
                systemChannel.sendPacket(EmptyPacket())
                systemChannel.nextPacket() //Wait a response packet (EmptyPacket) before closing the connection.
            }
            systemChannel.close(reason)
        }

        tasksHandler.close(reason)
        socket.close(reason)
        manager.unregister(socket.remoteSocketAddress())

        closed = true
        println(s"thread '$getName' closed.")
    }

    private[server] def createSync(id: Int): SyncPacketChannel =
        new SyncPacketChannel(socket, identifier, server.identifier, id, channelCache, packetManager)

    private[server] def createAsync(id: Int): AsyncPacketChannel = {
        new AsyncPacketChannel(identifier, server.identifier, id, channelCache, socket)
    }

    private[server] def updateSocket(socket: Socket): Unit =
        this.socket.set(socket)

    private[connection] def sendDeflectedBytes(bytes: Array[Byte]): Unit = {
        socket.write(bytes.length.toString.getBytes ++ PacketManager.SizeSeparator ++ bytes)
    }

    private def handlePacket(packet: Packet): Unit = {
        if (isNotInitialised) {
            unhandledPackets += packet
            return
        }
        notifier.onPacketReceived(packet)
        packet match {
            case errorPacket: ErrorPacket if errorPacket.errorType == ErrorPacket.ABORT_TASK =>
                printErrorPacket(errorPacket)
                tasksHandler.skipCurrent(Reason.ERROR_OCCURRED)
            case init: TaskInitPacket => tasksHandler.handlePacket(init)
            case _: Packet => channelCache.injectPacket(packet)
        }
    }

    private def printErrorPacket(packet: ErrorPacket): Unit = {
        val identifier = tasksHandler.identifier
        val errorType = packet.errorType
        Console.err.println(s"Received error from relay '$identifier' of type '$errorType'")
        packet.printError()
        notifier.onSystemError(packet)
    }

    private def update(onPacketReceived: Packet => Unit): Unit = {
        try {
            listenNextConcernedPacket(onPacketReceived)
        } catch {
            case e: RelayException => executeError(e)
            case e: SocketException if e.getMessage == "Connection reset" =>
                Console.err.println(s"client '$identifier' disconnected.")
        }
    }

    private def listenNextConcernedPacket(event: Packet => Unit): Unit = {
        val bytes = packetReader.readNextPacketBytes()
        if (bytes == null)
            return

        val target = getTargetID(bytes)
        if (target == server.identifier) { //check if packet concerns server
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
            case SystemPacket.ClientClose => close(Reason.EXTERNAL_REQUEST)
            case SystemPacket.ServerClose => server.close(identifier, Reason.EXTERNAL_REQUEST)
            case _ => Console.err.println(s"Could not find action for order '$order'")
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
        notifier.onSystemError(e)
    }


    private def initialiseConnection(): TasksHandler = {
        setName(s"RP Connection (unknownId)")
        implicit val channel: SyncPacketChannel =
            new SyncPacketChannel(socket, "unknown", server.identifier, -6, channelCache, packetManager)
        channel.sendPacket(SystemPacket(SystemPacket.ClientInitialisation))
        deflectInChannel(channel)

        new ConnectionTasksHandler(handleClientResponse(channel), server, socket)
    }

    private def handleClientResponse(implicit channel: SyncPacketChannel): String = {
        channel.nextPacket() match {
            case dataPacket: DataPacket =>
                val identifier = dataPacket.header
                val response = if (manager.containsIdentifier(identifier)) "ERROR" else "OK"
                channel.sendPacket(DataPacket(response))
                channel.close(Reason.LOCAL_REQUEST)

                if (response == "ERROR")
                    throw new RelayException("a Relay point connection have been rejected.")

                println(s"Relay Point connected with identifier '$identifier'")
                setName(s"RP Connection ($identifier)")
                identifier
            case other =>
                val name = other.getClass.getSimpleName
                throw new UnexpectedPacketException(s"Unexpected packet type $name received while initialising client connection.")
        }
    }


    private def deflectInChannel(channel: PacketChannelManager): Unit = update {
        case concerned: Packet if concerned.channelID == channel.channelID => channel.addPacket(concerned)
        case other: Packet =>
            handlePacket(other)
            deflectInChannel(channel)
    }

    private def isNotInitialised: Boolean = identifier == null

    private def getTargetID(bytes: Array[Byte]): String =
        PacketUtils.cutString(PacketManager.SenderSeparator, PacketManager.TargetSeparator)(bytes)

}
