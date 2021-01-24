package fr.`override`.linkit.server.connection

import java.net.Socket

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.concurency.{PacketWorkerThread, RelayWorkerThread}
import fr.`override`.linkit.api.exception.RelayException
import fr.`override`.linkit.api.network.{ConnectionState, RemoteConsole}
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.task.TasksHandler
import org.jetbrains.annotations.NotNull

import scala.util.control.NonFatal

class ClientConnection private(session: ClientConnectionSession) extends JustifiedCloseable {

    val identifier: String = session.identifier

    private val server = session.server
    private val packetTranslator = server.packetTranslator
    private val manager: ConnectionsManager = server.connectionsManager

    private val workerThread = new RelayWorkerThread()

    @volatile private var closed = false

    def start(): Unit = {
        if (closed) {
            throw new RelayException("This Connection was already used and is now definitely closed.")
        }
        ConnectionPacketWorker.start()
    }

    override def close(reason: CloseReason): Unit = {
        RelayWorkerThread.checkCurrentIsWorker()
        closed = true
        if (reason.isInternal && isConnected) {
            val sysChannel = session.channel
            sysChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }
        ConnectionPacketWorker.close(reason)

        session.close(reason)

        manager.unregister(identifier)
        workerThread.close()
        println(s"Connection closed for $identifier")
    }

    def sendPacket(packet: Packet, channelID: Int): Unit = runLater {
        val bytes = packetTranslator.fromPacketAndCoords(packet, PacketCoordinates(channelID, identifier, server.identifier))
        session.send(bytes)
    }

    def isConnected: Boolean = getState == ConnectionState.CONNECTED

    def getTasksHandler: TasksHandler = session.tasksHandler

    def getConsoleOut: RemoteConsole = session.outConsole

    def getConsoleErr: RemoteConsole = session.errConsole

    def getState: ConnectionState = session.getSocketState

    def addConnectionStateListener(action: ConnectionState => Unit): Unit = session.addStateListener(action)

    def openChannel[C <: PacketChannel](channelId: Int, factory: PacketChannelFactory[C]): C = {
        val channel = factory.createNew(session.traffic, channelId, identifier)
        session.traffic.register(channel)
        channel
    }

    def runLater(callback: => Unit): Unit = {
        workerThread.runLater(_ => callback)
    }

    override def isClosed: Boolean = closed

    private[server] def updateSocket(socket: Socket): Unit = {
        RelayWorkerThread.checkCurrentIsWorker()
        session.updateSocket(socket)
    }

    private[connection] def sendBytes(bytes: Array[Byte]): Unit = runLater {
        session.send(PacketUtils.wrap(bytes))
    }

    object ConnectionPacketWorker extends PacketWorkerThread {
        override protected def readAndHandleOnePacket(): Unit = {
            try {
                session.packetReader.nextPacket(handlePacket)
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
                    e.printStackTrace(session.errConsole)
                    runLater {
                        ClientConnection.this.close(CloseReason.INTERNAL_ERROR)
                    }
            }
        }

        private def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
            val containerID = coordinates.injectableID
            if (closed)
                return
            packet match {
                case systemError: ErrorPacket if containerID == PacketTraffic.SystemChannel => systemError.printError()
                case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
                case init: TaskInitPacket => session.tasksHandler.handlePacket(init, coordinates)
                case _: Packet =>
                    if (server.preHandlePacket(packet, coordinates))
                        session.traffic.injectPacket(packet, coordinates)
            }
        }


        private def handleSystemOrder(packet: SystemPacket): Unit = {
            val orderType = packet.order
            val reason = packet.reason.reversedPOV()
            val content = packet.content

            import SystemOrder._
            orderType match {
                case CLIENT_CLOSE => runLater(ClientConnection.this.close(reason))
                case SERVER_CLOSE => server.close(reason)
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
