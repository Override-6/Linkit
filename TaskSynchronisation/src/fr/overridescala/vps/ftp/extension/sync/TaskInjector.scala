package fr.overridescala.vps.ftp.`extension`.sync

import java.nio.file.Path

import fr.overridescala.vps.ftp.`extension`.fundamental.UploadTask
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescriptionBuilder}

class TaskInjector(private val relay: Relay) {

    def inject(extensionClass: Class[_ <: TaskExtension], folder: RelayTaskFolder): Unit = {
        val extensionURI = extensionClass.getProtectionDomain.getCodeSource.getLocation.toURI
        val extensionPath = Path.of(extensionURI)
        val transferDescription = new TransferDescriptionBuilder {
            destination = folder.path.toString
            source = FileDescription.fromLocal(extensionPath)
            targetID = folder.relayIdentifier
        }
        relay.scheduleTask(new UploadTask(transferDescription)).queue()
    }

}
