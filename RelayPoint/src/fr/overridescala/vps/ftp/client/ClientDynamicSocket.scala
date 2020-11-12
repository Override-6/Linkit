package fr.overridescala.vps.ftp.client

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket, SocketException}

import fr.overridescala.vps.ftp.api.packet.DynamicSocket
import fr.overridescala.vps.ftp.client.ClientDynamicSocket.AttemptSleepTime

class ClientDynamicSocket(boundAddress: InetSocketAddress) extends DynamicSocket {


    handleReconnection() //automatically connect

    private def newSocket(): Unit = {
        closeCurrentStreams()
        currentSocket = new Socket(boundAddress.getAddress, boundAddress.getPort)
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
    }

    override protected def handleReconnection(): Unit = {
        try {
            //FIXME added 5s cooldown for test only
            println(s"Waiting for $AttemptSleepTime ms before another try...")
            Thread.sleep(AttemptSleepTime)
            println("Reconnecting...")
            newSocket()
            println("Reconnected !")
        } catch {
            case _@(_: SocketException | _: ConnectException) => {
                println("Unable to connect to server.")
                println(s"Waiting for $AttemptSleepTime ms before another try...")
                Thread.sleep(AttemptSleepTime)
                handleReconnection()
            }
        }
    }
}

object ClientDynamicSocket {
    private val AttemptSleepTime = 5000
}
