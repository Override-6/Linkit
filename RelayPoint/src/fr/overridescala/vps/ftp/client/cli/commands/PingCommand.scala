package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.tasks.PingTask
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor}

class PingCommand(relay: Relay) extends CommandExecutor {
    override def execute(args: Array[String]): Unit = {
        if (args.length != 1)
            throw new CommandException("use : ping <target identifier>")
        val target = args(0)
        val ownIdentifier = relay.identifier
        println("pinging...")
        val ping = relay.scheduleTask(PingTask(target))
                .complete()
        println(s"$ownIdentifier <- $ping ms -> $target")
    }
}
