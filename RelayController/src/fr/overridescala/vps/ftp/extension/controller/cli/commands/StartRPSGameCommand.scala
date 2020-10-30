package fr.overridescala.vps.ftp.`extension`.controller.cli.commands

import fr.overridescala.vps.ftp.`extension`.ppc.logic.GameConfig
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandExecutor

class StartRPSGameCommand(relay: Relay) extends CommandExecutor {

    override def execute(implicit args: Array[String]): Unit = {
        new GameConfig(relay).displayConfigMenuThenStartGame()
    }

}
