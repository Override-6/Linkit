package fr.overridescala.vps.ftp.server.connection

import java.io._
import java.net.Socket

import fr.overridescala.vps.ftp.api.packet.DynamicSocket

class SocketContainer extends DynamicSocket {

    def set(socket: Socket): Unit = synchronized {
        if (currentSocket != null)
            closeCurrentStreams()

        currentSocket = socket
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
        notifyAll()
    }

    override protected def handleReconnection(): Unit = {
        println(s"Socket disconnected from ${remoteSocketAddress().getAddress.getHostAddress}")
        println("Reconnecting...")
        synchronized {
            wait()
        }
        println("Reconnected !")
    }

}