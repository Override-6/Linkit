package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.util.Scanner

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.Constants

object Main {

    private val SERVER_ADDRESS = new InetSocketAddress("161.97.104.230", Constants.PORT)

    def main(args: Array[String]): Unit = {
        runClient()
    }


    def runClient(): Unit = {
        print("say 'y' to connect to chose localhost : ")
        val isLocalhost = new Scanner(System.in).nextLine().startsWith("y")
        val address = if (isLocalhost) Constants.LOCALHOST else SERVER_ADDRESS
        val relayPoint: Relay = new RelayPoint(address, "client1")
        val source = "C:/Users/maxim/Desktop/Dev/VPS/transfertTests/client/MyVideos"
        val destinationUpload = "/home/override/VPS/Tests/FileTransferer/client-upload"
        val destinationDownload = "/home/override/VPS/Tests/FileTransferer/client-download"

        relayPoint.start()

        val serverAddress = relayPoint.requestAddress("server").completeNow()
        println(s"serverAddress = $serverAddress")

        /*val upload = TransferDescription.builder()
                .setSource(TransferableFile.fromLocal(source))
                .setDestination(destinationUpload)
                .setTarget(serverAddress)
                .build()
        relayPoint.doUpload(upload).completeNow()
         */
          val download = TransferDescription.builder()
                  .setSource(relayPoint.requestFileInformation(serverAddress, destinationUpload).completeNow())
                  .setDestination(source)
                  .setTarget(serverAddress)
                  .build()
         relayPoint.doDownload(download).completeNow()

    }


    def runTests(address: InetSocketAddress, source: String, destination: String): Unit = {
        val relayPoint: Relay = new RelayPoint(address, "client1")

        relayPoint.start()

        val serverAddress = relayPoint.requestAddress("server").completeNow()
        println(s"serverAddress = $serverAddress")

        val upload = TransferDescription.builder()
                .setSource(TransferableFile.fromLocal(source))
                .setDestination(destination)
                .setTarget(serverAddress)
                .build()
        relayPoint.doUpload(upload).completeNow()
      /*  val download = TransferDescription.builder()
                .setSource(relayPoint.requestFileInformation(serverAddress, destination).completeNow())
                .setDestination(source)
                .setTarget(serverAddress)
                .build()
       */

    }

}