package fr.`override`.linkit.client

import fr.`override`.linkit.core.connection.packet.traffic.DynamicSocket
import fr.`override`.linkit.core.local.system.ContextLogger

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket, SocketException}

class ClientDynamicSocket(boundAddress: InetSocketAddress,
                          socketFactory: InetSocketAddress => Socket) extends DynamicSocket(true, ) {

    override val boundIdentifier: String = Relay.ServerIdentifier

    var reconnectionPeriod: Int = 5000

    private def newSocket(): Unit = {
        closeCurrentStreams()
        currentSocket = socketFactory(boundAddress)
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
    }

    override protected def handleReconnection(): Unit = {
        try {
            newSocket()
        } catch {
            case _@(_: SocketException | _: ConnectException) =>
                ContextLogger.warn("Unable to connect to server.")
                ContextLogger.warn(s"Waiting for $reconnectionPeriod ms before another try...")
                Thread.sleep(reconnectionPeriod)
                handleReconnection()
        }
        markAsConnected()
    }

    def start(): Unit = {
        try {
            newSocket()
        } catch {
            case _@(_: SocketException | _: ConnectException) =>
            handleReconnection()
        }
        markAsConnected()
    }
}
