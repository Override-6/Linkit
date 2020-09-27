package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.tasks.CreateFileTask
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor}
import fr.overridescala.vps.ftp.client.cli.CommandUtils._

/**
 * syntax : <p>
 *  crtf "path" [-D] -t "relay identifier"
 *
 * */
class CreateFileCommand(relay: Relay) extends CommandExecutor {


    override def execute(args: Array[String]): Unit = {
        checkArgs(args)
        val path = args(0)
        val isDirectory = args.contains("-D")
        val target = argAfter(args, "-t")
        relay.scheduleTask(CreateFileTask.concoct(target, path, isDirectory))
                .complete()
    }

    def checkArgs(args: Array[String]): Unit = {
        if (args.length != 3 && args.length != 4)
            throw new CommandException("args length expected is 3 or 4")
        checkArgsContains(args, "-t")
    }
}
