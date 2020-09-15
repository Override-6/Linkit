package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.util.Scanner

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.Constants

object Main {

    private val SERVER_ADDRESS = new InetSocketAddress("161.97.104.230", Constants.PORT)

    print("say 'y' to connect to chose localhost : ")
    private val isLocalhost = new Scanner(System.in).nextLine().startsWith("y")
    private val address = if (isLocalhost) Constants.LOCALHOST else SERVER_ADDRESS
    private val relayPoint: Relay = new RelayPoint(address, "client1")
    private val source = "C:/Users/maxim/Desktop/Dev/VPS/transfertTests/client/MyVideos"
    private val destinationUpload = "/home/override/VPS/Tests/FileTransferer/client-upload"



    def main(args: Array[String]): Unit =
        runClient()

    def runClient(): Unit = {
        relayPoint.start()

        val serverAddress = relayPoint.requestAddress("server").completeNow()
        println(s"serverAddress = $serverAddress")

        relayPoint.requestFileInformation(serverAddress, destinationUpload)
                .queueWithSuccess(nextStep)
    }

    def nextStep(fileInfo: TransferableFile): Unit = {
        println("a")
        val download = TransferDescription.builder()
                .setSource(fileInfo)
                .setDestination(source)
                .setTarget(address)
                .build()
        val upload = TransferDescription.builder()
                .setSource(TransferableFile.fromLocal(source))
                .setDestination(destinationUpload)
                .setTarget(address)
                .build()
        relayPoint.doDownload(download).queue(e => println("le fichier a été download"), Console.err.println)
        relayPoint.doUpload(upload).queue(e => println("le fichier a été upload"), Console.err.println)
        println("toutes les tâches ont étées ajoutées et vont être éxécutées")
    }

}