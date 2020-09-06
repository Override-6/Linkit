package fr.overridescala.vps.ftp.client

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.Constants

object Main {

    def main(args: Array[String]): Unit = {
        val relayPoint: Relay = new RelayPoint("client1", Constants.PUBLIC_ADDRESS)
        relayPoint.start()

        val serverAddress = relayPoint.requestAddress("server").complete()
        println(s"serverAddress = ${serverAddress}")
        val serverFile = relayPoint.requestFileInformation(serverAddress, "C:/Users/maxim/Desktop/Dev/VPS/transfertTests/server/mavidaio.mp4").complete()
        println(s"serverFile = ${serverFile}")

        val upload = TransferDescription.builder()
                .setSource(TransferableFile.fromLocal("C:/Users/maxim/Desktop/Dev/VPS/transfertTests/client/client.mp4"))
                .setDestination("C:/Users/maxim/Desktop/Dev/VPS/transfertTests/server/clientUploadTest.mp4")
                .setTarget(serverAddress)
                .build()
        relayPoint.requestUpload(upload).complete()
    }

}
