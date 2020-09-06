package fr.overridescala.vps.ftp.api

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.task.Task
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}

trait Relay extends AutoCloseable {

    val identifier: String

    def doDownload(description: TransferDescription): Task[Unit]

    def doUpload(description: TransferDescription): Task[Unit]

    def requestAddress(id: String): Task[InetSocketAddress]

    def requestFileInformation(owner: InetSocketAddress, path: String): Task[TransferableFile]

    def start():Unit

}
