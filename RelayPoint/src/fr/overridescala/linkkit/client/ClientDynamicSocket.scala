package fr.overridescala.linkkit.client

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket, SocketException}

import fr.overridescala.linkkit.api.system.event.EventObserver.EventNotifier
import fr.overridescala.linkkit.api.packet.DynamicSocket
import ClientDynamicSocket.AttemptSleepTime

class ClientDynamicSocket(boundAddress: InetSocketAddress, notifier: EventNotifier) extends DynamicSocket(notifier) {


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
                println(s"Waiting for $AttemptSleepTime ms before another try...")
                Thread.sleep(AttemptSleepTime)
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

object ClientDynamicSocket {
    private val AttemptSleepTime = 5000
}
