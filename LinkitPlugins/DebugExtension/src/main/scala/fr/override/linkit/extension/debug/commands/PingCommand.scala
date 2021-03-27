package fr.`override`.linkit.extension.debug.commands

import fr.`override`.linkit.extension.controller.cli.{CommandException, CommandExecutor}
import fr.`override`.linkit.extension.debug.PingTask


class PingCommand(relay: Relay) extends CommandExecutor {
    override def execute(implicit args: Array[String]): Unit = {
        if (args.length != 1)
            throw CommandException("use : ping <target identifier>")
        val target = args(0)
        val ownIdentifier = relay.identifier
        println("pinging...")
        relay.scheduleTask(PingTask(target))
                .queue(
                    ping => println(s"$ownIdentifier <- $ping ms -> $target"),
                    errorMsg => Console.err.println(errorMsg)
                )
    }
}
