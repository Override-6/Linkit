package fr.overridescala.vps.ftp.`extension`.debug

import fr.overridescala.vps.ftp.`extension`.controller.ControllerExtension
import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandManager
import fr.overridescala.vps.ftp.`extension`.debug._
import fr.overridescala.vps.ftp.`extension`.debug.commands.{PingCommand, SendMessageCommand, StressTestCommand}
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.`extension`.{RelayExtension, relayExtensionInfo}

@relayExtensionInfo(name = "DebugExtension", dependencies = Array("RelayControllerCli"))
class DebugExtension(relay: Relay) extends RelayExtension(relay) {
    override def main(): Unit = {
        val completerHandler = relay.taskCompleterHandler

        completerHandler.putCompleter(PingTask.Type, _ => PingTask.Completer())
        completerHandler.putCompleter(StressTestTask.Type, StressTestTask.Completer)
        completerHandler.putCompleter(SendMessageTask.Type, SendMessageTask.Completer)

        val properties = relay.properties
        val commandManager = properties.getProperty(ControllerExtension.CommandManagerProp): CommandManager

        commandManager.register("ping", new PingCommand(relay))
        commandManager.register("stress", new StressTestCommand(relay))
        commandManager.register("msg", new SendMessageCommand(relay))
    }
}
