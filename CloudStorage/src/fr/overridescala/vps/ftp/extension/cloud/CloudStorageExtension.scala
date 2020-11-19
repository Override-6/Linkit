package fr.overridescala.vps.ftp.`extension`.cloud

import fr.overridescala.vps.ftp.`extension`.cloud.commands.{SyncDirCommand, TransferCommand}
import fr.overridescala.vps.ftp.`extension`.cloud.tasks.{DownloadTask, SyncFoldersTask, UploadTask}
import fr.overridescala.vps.ftp.`extension`.controller.ControllerExtension
import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandManager
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.`extension`.{RelayExtension, relayExtensionInfo}
import fr.overridescala.vps.ftp.api.utils.Utils

@relayExtensionInfo(name = "CloudStorageExtension", dependencies = Array("RelayControllerCli"))
class CloudStorageExtension(relay: Relay) extends RelayExtension(relay) {

      override def main(): Unit = {
            val completerHandler = relay.taskCompleterHandler
            completerHandler.putCompleter(UploadTask.TYPE, init => DownloadTask(Utils.deserialize(init.content)))
            completerHandler.putCompleter(DownloadTask.TYPE, init => UploadTask(Utils.deserialize(init.content)))
            completerHandler.putCompleter(SyncFoldersTask.TYPE, init => new SyncFoldersTask.Completer(relay, init))

            val properties = relay.properties
            val commandManager = properties.getProperty(ControllerExtension.CommandManagerProp): CommandManager
            commandManager.register("sync", new SyncDirCommand(relay))
            commandManager.register("download", TransferCommand.download(relay))
            commandManager.register("upload", TransferCommand.upload(relay))
      }

}