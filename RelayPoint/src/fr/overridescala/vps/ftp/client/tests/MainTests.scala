package fr.overridescala.vps.ftp.client.tests

import java.io.File
import java.nio.file.Path

import fr.overridescala.vps.ftp.api.packet.ext.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription, TransferDescriptionBuilder}
import fr.overridescala.vps.ftp.api.utils.Utils

object MainTests {

    def main(args: Array[String]): Unit = {

        val toUpload = "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\tests\\algoTests\\toUpload\\yeahYouRight\\open.txt"
        val destinationFolder = "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\tests\\algoTests\\A\\B\\C\\D"
        val fakeDesc = new TransferDescriptionBuilder {
            destination = destinationFolder
            source = FileDescription.fromLocal("C:\\Users\\maxim\\Desktop\\Dev\\VPS\\tests\\algoTests\\toUpload\\yeahYouRight")
            targetID = ""
        }
        val packet = new DataPacket(-1, "", "", "UPF", toUpload.getBytes)
        println(findDownloadPath(packet, fakeDesc))

    }

    private def findDownloadPath(packet: DataPacket, desc: TransferDescription): Path = {
        Utils.checkPacketHeader(packet, Array("UPF"))
        val root = Utils.formatPath(desc.source.rootPath)
        val rootNameCount = root.toString.count(char => char == File.separatorChar)

        val uploadedFile = Utils.formatPath(new String(packet.content))

        val destination = Utils.formatPath(new String(desc.destination))

        val relativePath = Utils.subPathOfUnknownFile(uploadedFile, rootNameCount)
        Utils.formatPath(destination.toString + relativePath)
    }

}
