package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.transfer.TransferDescription
import fr.overridescala.vps.ftp.api.utils.Constants

object Main {

    def main(args: Array[String]): Unit = {
        runClient()
    }


    def runClient(): Unit = {
        print("say 'y' to connect to chose localhost ")
        val isLocalhost = System.in.read() == 'y'
        val address = if (isLocalhost) Constants.LOCALHOST else new InetSocketAddress("161.97.104.230", Constants.PORT)

        val relayPoint: Relay = new RelayPoint("client1", address)

        relayPoint.start()

        val serverAddress = relayPoint.requestAddress("server").complete()
        println(s"serverAddress = ${serverAddress}")
        val serverFile = relayPoint.requestFileInformation(serverAddress, "/home/override/VPS/Tests/FileTransferer/server.mp4").complete()
        println(s"serverFile = ${serverFile}")

        val download = TransferDescription.builder()
                .setSource(serverFile)
                .setDestination("C:/Users/maxim/Desktop/Dev/VPS/transfertTests/client/client.mp4")
                .setTarget(serverAddress)
                .build()
        //TODO handles multi tasks
        relayPoint.doDownload(download).queue(_, _)
        relayPoint.doDownload(download).queue(_, _)
        relayPoint.doDownload(download).queue(_, _)
    }

}
