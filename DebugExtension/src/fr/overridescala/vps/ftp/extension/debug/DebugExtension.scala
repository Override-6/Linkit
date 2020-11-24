package fr.overridescala.vps.ftp.`extension`.debug

import fr.overridescala.vps.ftp.`extension`.controller.ControllerExtension
import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandManager
import fr.overridescala.vps.ftp.`extension`.debug._
import fr.overridescala.vps.ftp.`extension`.debug.commands.{PingCommand, StressTestCommand}
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.`extension`.{RelayExtension, relayExtensionInfo}

@relayExtensionInfo(name = "FundamentalExtension", dependencies = Array("RelayControllerCli"))
class DebugExtension(relay: Relay) extends RelayExtension(relay) {
    override def main(): Unit = {
        val completerHandler = relay.taskCompleterHandler

        completerHandler.putCompleter(PingTask.TYPE, _ => new PingTask.Completer())
        completerHandler.putCompleter(StressTestTask.TYPE, StressTestTask.Completer)

        val properties = relay.properties
        val commandManager = properties.getProperty(ControllerExtension.CommandManagerProp): CommandManager

        commandManager.register("ping", new PingCommand(relay))
        commandManager.register("stress", new StressTestCommand(relay))

    }
}
