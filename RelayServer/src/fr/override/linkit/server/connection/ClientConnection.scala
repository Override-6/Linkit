package fr.`override`.linkit.server.connection

import java.net.Socket

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.concurency.AsyncExecutionContext
import fr.`override`.linkit.api.exception.RelayException
import fr.`override`.linkit.api.network.{ConnectionState, RemoteConsole}
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.task.TasksHandler
import org.jetbrains.annotations.NotNull

import scala.concurrent.Future
import scala.util.control.NonFatal

class ClientConnection private(session: ClientConnectionSession) extends JustifiedCloseable {

    val identifier: String = session.identifier

    private val server = session.server
    private val packetTranslator = server.packetTranslator
    private val manager: ConnectionsManager = server.connectionsManager

    private val connectionThread = new Thread(server.packetWorkerThreadGroup, () => run(), s"Dedicated Packet Worker ($identifier)")

    @volatile private var closed = false

    def start(): Unit = {
        if (closed)
            throw new RelayException("This Connection was already used and is now definitely closed.")
        connectionThread.start()
    }

    override def close(reason: CloseReason): Unit = {
        closed = true

        if (reason.isInternal && isConnected) {
            session.channel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }

        session.close(reason)
        manager.unregister(identifier)
        connectionThread.interrupt()

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

    def sendPacket(packet: Packet, channelID: Int): Unit = {
        val bytes = packetTranslator.fromPacketAndCoords(packet, PacketCoordinates(channelID, identifier, server.identifier))
        session.send(bytes)
    }

    private[server] def updateSocket(socket: Socket): Unit =
        session.updateSocket(socket)

    //TODO Make all socket operation on the client thread
    //FIXME Using AsyncExecutionContext may block if more than 5 relays are connecting
    private[connection] def sendBytes(bytes: Array[Byte]): Unit = Future {
        println(s"Sending bytes to $identifier")
        session.send(PacketUtils.wrap(bytes))
    } (AsyncExecutionContext)

    private def run(): Unit = {
        val threadName = connectionThread.getName
        println(s"Thread '$threadName' started")
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
            case CLIENT_CLOSE => close(reason)
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

    override def isClosed: Boolean = closed
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
