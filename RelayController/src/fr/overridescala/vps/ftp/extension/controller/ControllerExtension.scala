package fr.overridescala.vps.ftp.`extension`.controller

import fr.overridescala.vps.ftp.`extension`.controller.ControllerExtension.{AutomationManagerProp, CommandManagerProp}
import fr.overridescala.vps.ftp.`extension`.controller.auto.AutomationManager
import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandManager
import fr.overridescala.vps.ftp.`extension`.controller.cli.commands._
import fr.overridescala.vps.ftp.`extension`.controller.tasks.SyncFoldersTask
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.`extension`.{RelayExtension, RelayExtensionInfo}


@RelayExtensionInfo(dependencies = Array("FundamentalExtension"), name = "RelayControllerCli")
class ControllerExtension(relay: Relay) extends RelayExtension(relay) {

    private val automationManager = new AutomationManager()
    private val commandManager = new CommandManager()

    override def main(): Unit = {
        commandManager.register("download", TransferCommand.download(relay))
        commandManager.register("upload", TransferCommand.upload(relay))
        commandManager.register("crtf", new CreateFileCommand(relay))
        commandManager.register("ping", new PingCommand(relay))
        commandManager.register("stress", new StressTestCommand(relay))
        commandManager.register("exec", new ExecuteUnknownTaskCommand(relay))
        commandManager.register("delete", new DeleteFileCommand(relay))
        commandManager.register("sync", new SyncDirCommand(relay))
        commandManager.register("stop", new ShutdownCommand())
        commandManager.start()
        automationManager.start()

        val completerHandler = relay.taskCompleterHandler
        completerHandler.putCompleter(SyncFoldersTask.TYPE, new SyncFoldersTask.Completer(relay, _))
        val properties = relay.properties
        properties.putProperty(AutomationManagerProp, automationManager)
        properties.putProperty(CommandManagerProp, commandManager)
    }

}

object ControllerExtension {
    val AutomationManagerProp: String = "automation_manager"
    val CommandManagerProp: String = "command_manager"
}
