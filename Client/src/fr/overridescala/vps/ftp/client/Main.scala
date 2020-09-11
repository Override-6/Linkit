package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.Constants

object Main {

    def main(args: Array[String]): Unit = {
        runClient()
    }


    def runClient(): Unit = {
        print("say 'y' to connect to chose localhost ")
        val isLocalhost = System.in.read() == 'y'

        if (isLocalhost)
            runLocalhostTests()
        else runOnlineTests()
    }

    def runLocalhostTests(): Unit = {
        runTests(Constants.LOCALHOST,
            "C:/Users/maxim/Desktop/Dev/VPS/transfertTests/client/client.mp4",
            "C:/Users/maxim/Desktop/Dev/VPS/transfertTests/server/clientToServer.mp4")
    }

    def runOnlineTests(): Unit = {
        runTests(new InetSocketAddress("161.97.104.230", Constants.PORT),
            "C:/Users/maxim/Desktop/Dev/VPS/transfertTests/client/client.mp4",
            "/home/override/VPS/Tests/FileTransferer/clientToServer.mp4")
    }

    def runTests(address: InetSocketAddress, source: String, destination: String): Unit = {
        val relayPoint: Relay = new RelayPoint(address, "client1")

        relayPoint.start()

        val serverAddress = relayPoint.requestAddress("server").completeNow()
        println(s"serverAddress = ${serverAddress}")

        val upload = TransferDescription.builder()
                .setSource(TransferableFile.fromLocal(source))
                .setDestination(destination)
                .setTarget(serverAddress)
                .build()
        relayPoint.doUpload(upload).queueWithError(msg => Console.print(msg))
    }

}