package fr.`override`.linkit.client

import fr.`override`.linkit.skull.Relay

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket, SocketException}

class ClientDynamicSocket(boundAddress: InetSocketAddress,
                          reconnectionPeriod: Int) extends DynamicSocket(true) {

    override val boundIdentifier: String = Relay.ServerIdentifier

    private def newSocket(): Unit = {
        closeCurrentStreams()
        currentSocket = new Socket(boundAddress.getAddress, boundAddress.getPort)
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
    }

    override protected def handleReconnection(): Unit = {
        try {
            newSocket()
        } catch {
            case _@(_: SocketException | _: ConnectException) =>
                Relay.Log.warn("Unable to connect to server.")
                Relay.Log.warn(s"Waiting for $reconnectionPeriod ms before another try...")
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
