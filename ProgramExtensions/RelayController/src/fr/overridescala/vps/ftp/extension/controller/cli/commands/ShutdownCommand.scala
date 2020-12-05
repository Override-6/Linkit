package fr.overridescala.vps.ftp.`extension`.controller.cli.commands

import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandExecutor

class ShutdownCommand extends CommandExecutor {
    override def execute(implicit args: Array[String]): Unit = System.exit(0)
}
