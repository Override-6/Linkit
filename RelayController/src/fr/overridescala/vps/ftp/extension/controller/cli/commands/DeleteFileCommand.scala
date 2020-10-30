package fr.overridescala.vps.ftp.`extension`.controller.cli.commands

import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandException
import fr.overridescala.vps.ftp.`extension`.fundamental.DeleteFileTask
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.`extension`.controller.cli.{CommandException, CommandExecutor}

class DeleteFileCommand(relay: Relay) extends CommandExecutor {

    override def execute(implicit args: Array[String]): Unit = {
        if (args.length != 2)
            throw new CommandException("use : delete <target> <path>")
        val target = args(0)
        val path = args(1)
        relay.scheduleTask(new DeleteFileTask(target, path)).queue(
            onSuccess = e => println("success !"),
            onError = msg => Console.err.println(msg)
        )
    }

}
