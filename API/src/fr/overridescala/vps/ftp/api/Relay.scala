package fr.overridescala.vps.ftp.api

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.task.{Task, TaskAction}
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}

trait Relay extends AutoCloseable {

    val identifier: String

    def doDownload(description: TransferDescription): TaskAction[Unit]

    def doUpload(description: TransferDescription): TaskAction[Unit]

    def requestAddress(id: String): TaskAction[InetSocketAddress]

    def requestFileInformation(owner: InetSocketAddress, path: String): TaskAction[TransferableFile]

    def start():Unit

}
