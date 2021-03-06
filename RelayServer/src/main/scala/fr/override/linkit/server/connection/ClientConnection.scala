package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.concurrency.{PacketWorkerThread, RelayThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.exception.{RelayException, UnexpectedPacketException}
import fr.`override`.linkit.api.network.{ConnectionState, RemoteConsole}
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.fundamental.ValPacket.BooleanPacket
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.packet.traffic.PacketInjections
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.task.TasksHandler
import org.jetbrains.annotations.NotNull

import java.net.Socket
import scala.util.control.NonFatal

class ClientConnection private(session: ClientConnectionSession) extends JustifiedCloseable {

    val identifier: String = session.identifier

    private val server = session.server
    private val packetTranslator = server.packetTranslator
    private val manager: ConnectionsManager = server.connectionsManager

    private val workerThread = new RelayThreadPool("Packet Handling & Extension", 3)

    @volatile private var closed = false

    override def close(reason: CloseReason): Unit = {
        RelayThreadPool.checkCurrentIsWorker()
        closed = true
        if (reason.isInternal && isConnected) {
            val sysChannel = session.channel
            sysChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }
        ConnectionPacketWorker.close(reason)

        session.close(reason)

        manager.unregister(identifier)
        workerThread.close()
        Relay.Log.trace(s"Connection closed for $identifier")
    }

    def start(): Unit = {
        if (closed) {
            throw new RelayException("This Connection was already used and is now definitely closed.")
        }
        ConnectionPacketWorker.start()
    }

    def sendPacket(packet: Packet, channelID: Int): Unit = {
        runLater {
            val bytes = packetTranslator.fromPacketAndCoords(packet, DedicatedPacketCoordinates(channelID, identifier, server.identifier))
            session.send(bytes)
        }
    }

    def isConnected: Boolean = getState == ConnectionState.CONNECTED

    def getTasksHandler: TasksHandler = session.tasksHandler

    def getConsoleOut: RemoteConsole = session.outConsole

    def getConsoleErr: RemoteConsole = session.errConsole

    def getState: ConnectionState = session.getSocketState

    def addConnectionStateListener(action: ConnectionState => Unit): Unit = session.addStateListener(action)

    def runLater(callback: => Unit): Unit = {
        workerThread.runLater(callback)
    }

    private[server] def concludeInitialisation(): Unit = {
        val entity = server.network.getEntity(identifier).get
        val connectionApiVersion = entity.apiVersion

        if (connectionApiVersion != Relay.ApiVersion)
            Console.err.println("The api version of this relay differs from the api version of the server, some connectivity problems could occur")
    }

    override def isClosed: Boolean = closed

    private[server] def updateSocket(socket: Socket): Unit = {
        RelayThreadPool.checkCurrentIsWorker()
        session.updateSocket(socket)
    }

    private[connection] def sendBytes(bytes: Array[Byte]): Unit = {
        session.send(PacketUtils.wrap(bytes))
    }

    object ConnectionPacketWorker extends PacketWorkerThread {

        @relayWorkerExecution
        override protected def refresh(): Unit = {
            try {
                session
                        .packetReader
                        .nextPacket((packet, coordinates, packetNumber) => {
                            runLater(handlePacket(packet, coordinates, packetNumber))
                        })
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
                    e.printStackTrace(session.errConsole)
                    runLater {
                        ClientConnection.this.close(CloseReason.INTERNAL_ERROR)
                    }
            }
        }

        @relayWorkerExecution
        private def handlePacket(packet: Packet, coordinates: DedicatedPacketCoordinates, number: Int): Unit = {
            if (closed)
                return

            packet match {
                case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
                case init: TaskInitPacket => session.tasksHandler.handlePacket(init, coordinates)
                case _: Packet =>
                    val injection = PacketInjections.createInjection(packet, coordinates, number)
                    session.serverTraffic.handleInjection(injection)
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

                case _ => new UnexpectedPacketException(s"Could not complete order '$orderType', can't be handled by a server or unknown order")
                        .printStackTrace(getConsoleErr)
            }

            def checkIDRegistered(target: String): Unit = {
                session.channel.send(BooleanPacket(server.isConnected(target)))
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
