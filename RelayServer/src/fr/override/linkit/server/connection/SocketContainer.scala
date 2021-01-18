package fr.`override`.linkit.server.connection

import java.io._
import java.net.Socket

import fr.`override`.linkit.api.packet.traffic.DynamicSocket

class SocketContainer(autoReconnect: Boolean) extends DynamicSocket(autoReconnect) {

    override lazy val boundIdentifier: String = identifier
    var identifier: String = "$NOT SET$"

    def set(socket: Socket): Unit = synchronized {
        if (currentSocket != null && !autoReconnect)
            closeCurrentStreams()

        currentSocket = socket
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
        notifyAll()
        markAsConnected()
    }

    def get: Socket = currentSocket

    override protected def handleReconnection(): Unit = {
        synchronized {
            try {
                wait()
            } catch {
                case e:InterruptedException => //thrown when the reconnection is brutally stopped (ex: server stopped, critical error...)
            }
        }
    }

}