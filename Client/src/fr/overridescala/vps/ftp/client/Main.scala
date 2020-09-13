package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.util.Scanner
import java.util.regex.Pattern

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.Constants

import scala.io.StdIn

object Main {

    def main(args: Array[String]): Unit = {
        runClient()
    }


    def runClient(): Unit = {
        print("say 'y' to connect to chose localhost : ")
        val isLocalhost = new Scanner(System.in).nextLine().equals("y")

        if (isLocalhost)
            runLocalhostTests()
        else runOnlineTests()
    }

    def runLocalhostTests(): Unit = {
        runTests(Constants.LOCALHOST,
            "C:/Users/maxim/Desktop/Dev/VPS/transfertTests/client/client.mp4",
            "C:/Users/maxim/Desktop/Dev/VPS/transfertTests/server/server.mp4")
    }

    def runOnlineTests(): Unit = {
        val scanner = new Scanner(System.in)
        print("choose a local file : ")
        val source = scanner.nextLine()
        print("choose a file on the server : ")
        val destination = scanner.nextLine()

        runTests(new InetSocketAddress("161.97.104.230", Constants.PORT),
            source, destination)
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
        val download = TransferDescription.builder()
                .setSource(relayPoint.requestFileInformation(serverAddress, destination).completeNow())
                .setDestination(source)
                .setTarget(serverAddress)
                .build()

        relayPoint.doDownload(download).queueWithSuccess(msg => Console.out.println(msg))
        relayPoint.doUpload(upload).queueWithError(msg => Console.err.println(msg))
        relayPoint.doDownload(download).queueWithError(msg => Console.err.println(msg))
        relayPoint.doUpload(upload).queueWithError(msg => Console.err.println(msg))
        relayPoint.doDownload(download).queueWithError(msg => Console.err.println(msg))
        relayPoint.doUpload(upload).queueWithError(msg => Console.err.println(msg))
        relayPoint.doDownload(download).queueWithError(msg => Console.err.println(msg))
        relayPoint.doUpload(upload).queueWithError(msg => Console.err.println(msg))

    }

}