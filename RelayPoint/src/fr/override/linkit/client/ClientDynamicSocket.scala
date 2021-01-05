package fr.`override`.linkit.client

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket, SocketException}

import fr.`override`.linkit.api.packet.traffic.DynamicSocket

class ClientDynamicSocket(boundAddress: InetSocketAddress,
                          reconnectionPeriod: Int) extends DynamicSocket(true) {


    private def newSocket(): Unit = {
        closeCurrentStreams()
        currentSocket = new Socket(boundAddress.getAddress, boundAddress.getPort)
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
    }

    override protected def handleReconnection(): Unit = {
        try {
            println("Reconnecting...")
            newSocket()
            println("Reconnected !")
        } catch {
            case _@(_: SocketException | _: ConnectException) =>
                println("Unable to connect to server.")
                println(s"Waiting for $reconnectionPeriod ms before another try...")
                Thread.sleep(reconnectionPeriod)
                handleReconnection()
        }
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
