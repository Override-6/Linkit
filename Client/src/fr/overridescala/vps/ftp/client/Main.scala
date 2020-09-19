package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.util.Scanner

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, FileDescription}
import fr.overridescala.vps.ftp.api.utils.Constants

object Main {

    private val SERVER_ADDRESS = new InetSocketAddress("161.97.104.230", Constants.PORT)

    print("say 'y' to connect to chose localhost : ")
    private val scanner = new Scanner(System.in)
    private val isLocalhost = scanner.nextLine().startsWith("y")
    private val address = if (isLocalhost) Constants.LOCALHOST else SERVER_ADDRESS
    private val relayPoint: Relay = new RelayPoint(address, "client1")
    print("server folder to make your tests : ")
    private val serverFolderTest = scanner.nextLine()
    print("download folder : ")
    private val downloadFolder = scanner.nextLine()
    print("upload folder : ")
    private val uploadFolder = scanner.nextLine()


    def main(args: Array[String]): Unit = {
        runClient()
    }

    def runClient(): Unit = {
        relayPoint.start()

        relayPoint.requestCreateFile(Constants.SERVER_ID, serverFolderTest)
                .queue()
        val fileInfo = relayPoint.requestFileInformation(Constants.SERVER_ID, serverFolderTest)
                .complete()
        performDownload(fileInfo)
        //performUpload()
    }

    def performDownload(fileInfo: FileDescription): Unit = {
        val download = TransferDescription.builder()
                .setSource(fileInfo)
                .setDestination(downloadFolder)
                .setTargetID(Constants.SERVER_ID)
                .build()
        relayPoint.doDownload(download).queue(e => println("le fichier a été download"), Console.err.println)
    }

    def performUpload(): Unit = {
        val upload = TransferDescription.builder()
                .setSource(FileDescription.fromLocal(uploadFolder))
                .setDestination(serverFolderTest)
                .setTargetID(Constants.SERVER_ID)
                .build()
        relayPoint.doUpload(upload).queue(e => println("le fichier a été upload"), Console.err.println)
        println("toutes les tâches ont étées ajoutées et vont être éxécutées")
    }

}