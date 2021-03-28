package fr.linkit.plugin.controller.cli.commands

import fr.linkit.api.local.ApplicationContext
import fr.linkit.plugin.controller.cli.CommandExecutor

class ShutdownCommand(context: ApplicationContext) extends CommandExecutor {
    override def execute(implicit args: Array[String]): Unit = context.runLater {
        context.shutdown()
        System.exit(0)
    }
}