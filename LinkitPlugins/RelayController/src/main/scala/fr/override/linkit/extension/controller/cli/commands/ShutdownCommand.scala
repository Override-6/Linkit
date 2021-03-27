package fr.`override`.linkit.extension.controller.cli.commands

import fr.`override`.linkit.extension.controller.cli.CommandExecutor

class ShutdownCommand(relay: Relay) extends CommandExecutor {
    override def execute(implicit args: Array[String]): Unit = relay.runLater {
        relay.close(Reason.INTERNAL)
        System.exit(0)
    }
}
