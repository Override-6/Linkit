package fr.overridescala.vps.ftp.api.tests

import java.net.InetSocketAddress
import java.nio.file.Path

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.Task
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}

class RelayMock extends Relay {

    override def requestDownload(description: TransferDescription): Task[Unit] = ???

    override def requestUpload(description: TransferDescription): Task[Unit] = ???

    override def requestAddress(id: String): Task[InetSocketAddress] = ???

    override def requestFileInformation(owner: InetSocketAddress, path: String): Task[TransferableFile] = ???

    override def start(): Unit = ???

    override def close(): Unit = ???

    override val identifier: String = ""
}
