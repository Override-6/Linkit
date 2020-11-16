package fr.overridescala.vps.ftp.server.connection

import java.net.Socket

import fr.overridescala.vps.ftp.api.`extension`.packet.PacketManager
import fr.overridescala.vps.ftp.api.exceptions.{RelayException, UnexpectedPacketException}
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.fundamental._
import fr.overridescala.vps.ftp.api.system._
import fr.overridescala.vps.ftp.api.task.TasksHandler
import fr.overridescala.vps.ftp.server.RelayServer
import fr.overridescala.vps.ftp.server.task.ConnectionTasksHandler

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ClientConnectionThread private(socket: SocketContainer,
                                     server: RelayServer,
                                     val identifier: String) extends Thread with JustifiedCloseable {


    private val packetManager = server.packetManager
    private val packetReader = new ServerPacketReader(socket, server, identifier)
    private val notifier = server.eventDispatcher.notifier
    private val channelsHandler = new PacketChannelsHandler(notifier, socket, packetManager)
    private implicit val systemChannel: SystemPacketChannel = new SystemPacketChannel(identifier, server.identifier, channelsHandler)


    /**
     * This array is only used and updated while the connection is not initialised.
     * */
    private val unhandledPackets = ListBuffer.empty[Packet]
    private val manager: ConnectionsManager = server.connectionsManager
    val tasksHandler: TasksHandler = new ConnectionTasksHandler(identifier, server, systemChannel)


    @volatile private var closed = false

    override def run(): Unit = {
        if (closed)
            throw new RelayException("This Connection was already used and is now definitely closed")

        println(s"Thread '$getName' was started")

        unhandledPackets.foreach(handlePacket)
        unhandledPackets.clear()

        try {
            while (!closed)
                packetReader.nextPacket(handlePacket)
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
            if (reason.isInternal) {
                systemChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
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
            case systemPacket: SystemPacket => handleSystemOrder(systemPacket.order, systemPacket.reason.reversed())
            case init: TaskInitPacket => tasksHandler.handlePacket(init)
            case _: Packet => channelsHandler.injectPacket(packet)
        }
    }


    def handleSystemOrder(orderType: SystemOrder, reason: Reason): Unit = {
        println(s"order $orderType had been requested by '$identifier'")
        orderType match {
            case SystemOrder.CLIENT_CLOSE => close(reason)
            case SystemOrder.SERVER_CLOSE => server.close(identifier, reason)
            case SystemOrder.ABORT_TASK => tasksHandler.skipCurrent(reason)
            case _ => systemChannel.sendPacket(ErrorPacket("forbidden order", s"could not complete order '$orderType', can't be handled by a server or unknown order"))
        }
    }

    private def isNotInitialised: Boolean = identifier == null

    setName(s"RP Connection ($identifier)")

}

object ClientConnectionThread {

    def open(socket: SocketContainer,
             server: RelayServer): ClientConnectionThread = {
        val packetReader = new ServerPacketReader(socket, server, null)
        val unhandledPackets = ListBuffer.empty[Packet]
        val tempChannelsHandler = new PacketChannelsHandler(server.notifier, socket, server.packetManager)
        val connectionsManager = server.connectionsManager

        def deflectInChannel(channel: PacketChannelManager): Unit = packetReader.nextPacket {
            case concerned: Packet if concerned.channelID == channel.channelID => channel.addPacket(concerned)
            case other: Packet =>
                unhandledPackets += other
                deflectInChannel(channel)
        }

        def handleClientResponse(implicit channel: SyncPacketChannel): String = {
            channel.nextPacket() match {
                case dataPacket: DataPacket =>
                    val identifier = dataPacket.header
                    val response = if (connectionsManager.containsIdentifier(identifier)) "ERROR" else "OK"
                    channel.sendPacket(DataPacket(response))

                    if (response == "ERROR")
                        throw new RelayException("a Relay point connection have been rejected.")

                    println(s"Relay Point connected with identifier '$identifier'")
                    identifier
                case other =>
                    val name = other.getClass.getSimpleName
                    throw new UnexpectedPacketException(s"Unexpected packet type $name received while initialising client connection.")
            }
        }

        implicit val channel: SyncPacketChannel =
            new SyncPacketChannel("unknown", server.identifier, 6, tempChannelsHandler)

        channel.sendPacket(SystemPacket(SystemOrder.CLIENT_INITIALISATION, Reason.INTERNAL))
        deflectInChannel(channel)

        val identifier = handleClientResponse(channel)
        channel.close(Reason.INTERNAL)
        val connection = new ClientConnectionThread(socket, server, identifier)

        unhandledPackets.foreach(connection.handlePacket)
        connection.start()

        connection
    }

}
