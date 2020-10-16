package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.`extension`.fundamental.PingTask
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor}

class PingCommand(relay: Relay) extends CommandExecutor {
    override def execute(implicit args: Array[String]): Unit = {
        if (args.length != 1)
            throw new CommandException("use : ping <target identifier>")
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
