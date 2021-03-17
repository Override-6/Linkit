package fr.`override`.linkit.server.connection

import java.io._
import java.net.Socket

class SocketContainer(autoReconnect: Boolean) extends DynamicSocket(autoReconnect) {

    override def boundIdentifier: String = identifier
    var identifier: String = "$NOT SET$"

    def set(socket: Socket): Unit = this.synchronized {
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
        this.synchronized {
            try {
                wait()
            } catch {
                case _:InterruptedException => //thrown when the reconnection is brutally stopped (ex: server stopped, critical error...)
            }
        }
    }

}