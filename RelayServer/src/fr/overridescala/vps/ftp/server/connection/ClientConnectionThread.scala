package fr.overridescala.vps.ftp.server.connection

import java.net.{Socket, SocketException}

import fr.overridescala.vps.ftp.api.`extension`.packet.{PacketManager, PacketUtils}
import fr.overridescala.vps.ftp.api.exceptions.{RelayException, UnexpectedPacketException}
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.fundamental._
import fr.overridescala.vps.ftp.api.system._
import fr.overridescala.vps.ftp.api.task.TasksHandler
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
    private val channelsHandler = new PacketChannelsHandler(notifier, socket, packetManager)

    /**
     * This array is only used and updated while the connection is not initialised.
     * */
    private val unhandledPackets = ListBuffer.empty[Packet]

    val tasksHandler: TasksHandler = initialiseConnection()
    val identifier: String = tasksHandler.identifier //shortcut

    private implicit val systemChannel: SystemPacketChannel = new SystemPacketChannel(identifier, server.identifier, channelsHandler)

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
            case NonFatal(e) => e.printStackTrace()
        }
        println(s"End of Thread execution '$getName'")
    }

    override def close(reason: Reason): Unit = {
        println(s"closing thread '$getName'")
        if (socket.isConnected) {
            if (reason.isLocal) {
                systemChannel.sendOrder(SystemOrder.CLIENT_INITIALISATION)
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
        new SyncPacketChannel(identifier, server.identifier, id, channelsHandler)

    private[server] def createAsync(id: Int): AsyncPacketChannel = {
        new AsyncPacketChannel(identifier, server.identifier, id, channelsHandler)
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
            case systemError: ErrorPacket if packet.channelID == systemChannel.channelID => systemError.printError()
            case systemPacket: SystemPacket => handleSystemOrder(systemPacket.order)
            case init: TaskInitPacket => tasksHandler.handlePacket(init)
            case _: Packet => channelsHandler.injectPacket(packet)
        }
    }

    private def update(onPacketReceived: Packet => Unit): Unit = {
        try {
            listenNextConcernedPacket(onPacketReceived)
        } catch {
            case e: RelayException => e.printStackTrace();
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
            event(packet)
            return
        }
        manager.deflectTo(bytes, target)
    }


    def handleSystemOrder(orderType: SystemOrder): Unit = {
        println(s"order $orderType had been requested by '$identifier'")
        orderType match {
            case SystemOrder.CLIENT_CLOSE => close(Reason)
            case SystemOrder.SERVER_CLOSE => server.close(identifier, Reason.EXTERNAL)
            case SystemOrder.ABORT_TASK => tasksHandler.skipCurrent(Reason.EXTERNAL)
            case _ => systemChannel.sendPacket(ErrorPacket("forbidden order", s"could not complete order '$orderType', can't be handled by a server or unknown order"))
        }
    }


    private def initialiseConnection(): TasksHandler = {
        setName(s"RP Connection (unknownId)")
        implicit val channel: SyncPacketChannel =
            new SyncPacketChannel("unknown", server.identifier, -6, channelsHandler)
        channel.sendPacket(SystemPacket(SystemOrder.CLIENT_INITIALISATION))
        deflectInChannel(channel)

        new ConnectionTasksHandler(handleClientResponse(channel), server, socket)
    }

    private def handleClientResponse(implicit channel: SyncPacketChannel): String = {
        channel.nextPacket() match {
            case dataPacket: DataPacket =>
                val identifier = dataPacket.header
                val response = if (manager.containsIdentifier(identifier)) "ERROR" else "OK"
                channel.sendPacket(DataPacket(response))
                channel.close(Reason.LOCAL)

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
