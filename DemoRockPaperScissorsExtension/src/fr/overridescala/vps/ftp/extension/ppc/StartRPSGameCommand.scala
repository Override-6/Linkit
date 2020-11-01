package fr.overridescala.vps.ftp.`extension`.ppc

import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandExecutor
import fr.overridescala.vps.ftp.`extension`.ppc.logic.GameConfig
import fr.overridescala.vps.ftp.api.Relay

class StartRPSGameCommand(relay: Relay) extends CommandExecutor {

    override def execute(implicit args: Array[String]): Unit = {
        new GameConfig(relay).displayConfigMenuThenStartGame()
    }

}
