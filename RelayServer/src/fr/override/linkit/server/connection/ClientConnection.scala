package fr.`override`.linkit.server.connection

import java.net.Socket

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.RelayException
import fr.`override`.linkit.api.network.ConnectionState
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.system.SystemPacketChannel.SystemChannelID
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.task.TasksHandler
import org.jetbrains.annotations.NotNull

import scala.util.control.NonFatal

class ClientConnection private(session: ClientConnectionSession) extends JustifiedCloseable {

    val identifier: String = session.identifier

    private val server = session.server
    private val packetManager = server.packetManager
    private val notifier = server.eventObserver.notifier
    private val manager: ConnectionsManager = server.connectionsManager
    private val serverTraffic = server.trafficHandler

    private val connectionThread = new Thread(server.packetWorkerThreadGroup, () => run(), s"Dedicated Packet Worker ($identifier)")

    @volatile private var closed = false

    def start(): Unit = {
        if (closed)
            throw new RelayException("This Connection was already used and is now definitely closed.")
        connectionThread.start()
    }

    override def close(reason: CloseReason): Unit = {
        if (reason.isInternal && isConnected) {
            session.channel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }

        session.close(reason)
        manager.unregister(identifier)
        connectionThread.interrupt()

        closed = true
    }

    def isConnected: Boolean = getState == ConnectionState.CONNECTED

    def getTasksHandler: TasksHandler = session.tasksHandler

    def getConsoleOut: RemoteConsole = session.outConsole

    def getConsoleErr: RemoteConsole = session.errConsole

    def getState: ConnectionState = session.getSocketState

    def addConnectionStateListener(action: ConnectionState => Unit): Unit = session.addStateListener(action)

    def createSync(id: Int, cacheSize: Int): PacketChannel.Sync =
        new SyncPacketChannel(identifier, id, cacheSize, session.traffic)

    def createAsync(id: Int): PacketChannel.Async = {
        new AsyncPacketChannel(identifier, id, session.traffic)
    }

    def sendPacket(packet: Packet, channelID: Int): Unit = {
        val bytes = packetManager.toBytes(packet, PacketCoordinates(channelID, identifier, server.identifier))
        session.send(bytes)
    }

    private[server] def updateSocket(socket: Socket): Unit =
        session.updateSocket(socket)

    private[connection] def sendDeflectedBytes(bytes: Array[Byte]): Unit = {
        session.send(PacketUtils.wrap(bytes))
    }

    private def run(): Unit = {
        val threadName = connectionThread.getName
        println(s"Thread '$threadName' was started")
        try {
            while (!closed)
                session.packetReader.nextPacket(handlePacket)
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                e.printStackTrace(session.errConsole)
                close(CloseReason.INTERNAL_ERROR)
        }
        println(s"End of Thread execution '$threadName'")
    }

    private def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val containerID = coordinates.containerID
        if (closed)
            return
        notifier.onPacketReceived(packet, coordinates)
        packet match {
            case systemError: ErrorPacket if containerID == SystemChannelID => systemError.printError()
            case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
            case init: TaskInitPacket => session.tasksHandler.handlePacket(init, coordinates)
            case _: Packet =>
                if (serverTraffic.isRegistered(coordinates.containerID)) {
                    serverTraffic.injectPacket(packet, coordinates)
                    return
                }
                session.traffic.injectPacket(packet, coordinates)
        }
    }


    private def handleSystemOrder(packet: SystemPacket): Unit = {
        val orderType = packet.order
        val reason = packet.reason.reversedPOV()
        val content = packet.content

        notifier.onSystemOrderReceived(orderType, reason)
        import SystemOrder._
        orderType match {
            case CLIENT_CLOSE => close(reason)
            case SERVER_CLOSE => server.close(identifier, reason)
            case ABORT_TASK => session.tasksHandler.skipCurrent(reason)
            case CHECK_ID => checkIDRegistered(new String(content))
            case PRINT_INFO => server.getConsoleOut(identifier).println(s"Connected to server ${server.relayVersion} (${Relay.ApiVersion})")

            case _ => session.channel.sendPacket(ErrorPacket("Forbidden order", s"Could not complete order '$orderType', can't be handled by a server or unknown order"))
        }

        def checkIDRegistered(target: String): Unit = {
            val response = if (server.isConnected(target)) "OK" else "ERROR"
            session.channel.sendPacket(DataPacket(response))
        }
    }

}

object ClientConnection {

    /**
     * Constructs a ClientConnection without starting it.
     *
     * @throws NullPointerException if the identifier or the socket is null.
     * @return a started ClientConnection.
     * @see [[SocketContainer]]
     * */
    def open(@NotNull session: ClientConnectionSession): ClientConnection = {
        if (session == null) {
            throw new NullPointerException("Unable to construct ClientConnection : session cant be null")
        }
        val connection = new ClientConnection(session)
        connection.start()
        connection
    }

}
