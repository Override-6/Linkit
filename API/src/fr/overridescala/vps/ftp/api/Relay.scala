package fr.overridescala.vps.ftp.api

import fr.overridescala.vps.ftp.api.task.TaskAction
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription}

trait Relay extends AutoCloseable {

    val identifier: String

    def doDownload(description: TransferDescription): TaskAction[Unit]

    def doUpload(description: TransferDescription): TaskAction[Unit]

    def requestFileInformation(ownerID: String, path: String): TaskAction[FileDescription]

    def requestCreateFile(ownerID: String, path: String): TaskAction[Unit]

    def start():Unit

}
