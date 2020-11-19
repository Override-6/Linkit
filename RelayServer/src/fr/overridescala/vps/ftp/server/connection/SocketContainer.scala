package fr.overridescala.vps.ftp.server.connection

import java.io._
import java.net.Socket

import fr.overridescala.vps.ftp.api.system.event.EventDispatcher.EventNotifier
import fr.overridescala.vps.ftp.api.packet.DynamicSocket

class SocketContainer(notifier: EventNotifier, closeStreamsOnRefresh: Boolean) extends DynamicSocket(notifier) {

    def set(socket: Socket): Unit = synchronized {
        if (currentSocket != null && closeStreamsOnRefresh)
            closeCurrentStreams()

        currentSocket = socket
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
        notifyAll()
        markAsConnected()
    }

    def get: Socket = currentSocket

    override protected def handleReconnection(): Unit = {
        val address = remoteSocketAddress().getAddress.getHostAddress
        println(s"Socket disconnected from $address")
        println("Reconnecting...")
        synchronized {
            wait()
        }
        println("Reconnected !")
    }

}