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
        println(s"Closing thread '$getName'")
        if (socket.isConnected && reason.isInternal) {
            systemChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }

        tasksHandler.close(reason)
        socket.close(reason)
        channelsHandler.close(reason)
        manager.unregister(identifier)

        closed = true
        println(s"Thread '$getName' closed.")
    }

    def isConnected: Boolean = socket.isConnected

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
            case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
            case init: TaskInitPacket => tasksHandler.handlePacket(init)
            case _: Packet => channelsHandler.injectPacket(packet)
        }
    }


    private def handleSystemOrder(packet: SystemPacket): Unit = {
        val orderType = packet.order
        val reason = packet.reason
        val content = packet.content

        println(s"Order $orderType had been requested by '$identifier'")
        notifier.onSystemOrderReceived(orderType, reason)
        orderType match {
            case SystemOrder.CLIENT_CLOSE => close(reason)
            case SystemOrder.SERVER_CLOSE => server.close(identifier, reason)
            case SystemOrder.ABORT_TASK => tasksHandler.skipCurrent(reason)
            case SystemOrder.CHECK_ID => checkIDRegistered(new String(content))

            case _ => systemChannel.sendPacket(ErrorPacket("forbidden order", s"could not complete order '$orderType', can't be handled by a server or unknown order"))
        }
    }

    private def checkIDRegistered(target: String): Unit = {
        val response = if (manager.containsIdentifier(target)) "OK" else "ERROR"
        systemChannel.sendPacket(DataPacket(response))
    }

    private def isNotInitialised: Boolean = identifier == null

    setName(s"RP Connection ($identifier)")

}

object ClientConnectionThread {

    def open(socket: SocketContainer, server: RelayServer, identifier: String = null): ClientConnectionThread = {
        val relayIdentifier = if (identifier == null) retrieveIdentifier(socket, server) else identifier
        val connection = new ClientConnectionThread(socket, server, relayIdentifier)
        connection.start()
        connection
    }

    def retrieveIdentifier(socket: SocketContainer, server: RelayServer): String = {
        val packetReader = new ServerPacketReader(socket, server, null)
        val tempChannelsHandler = new PacketChannelsHandler(server.notifier, socket, server.packetManager)

        val channel: SyncPacketChannel =
            new SyncPacketChannel("unknown", server.identifier, 6, tempChannelsHandler)

        def deflect(): Unit = packetReader.nextPacket {
            case concerned: Packet if concerned.channelID == channel.channelID => channel.addPacket(concerned)
            case _: Packet => deflect()
        }

        def handleClientResponse(): String = channel.nextPacket() match {
            case dataPacket: DataPacket => dataPacket.header
            case other =>
                val name = other.getClass.getSimpleName
                throw new UnexpectedPacketException(s"Unexpected packet type $name received while getting RelayPoint identifier.")
        }

        channel.sendPacket(SystemPacket(SystemOrder.GET_IDENTIFIER, Reason.INTERNAL)(channel))
        deflect()

        val identifier = handleClientResponse()
        channel.close(Reason.INTERNAL)

        identifier
    }

}
