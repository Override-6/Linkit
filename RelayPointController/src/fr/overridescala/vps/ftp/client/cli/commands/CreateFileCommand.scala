package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.`extension`.fundamental.CreateFileTask
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.client.cli.CommandUtils._
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor}

/**
 * syntax : <p>
 *  crtf "path" [-D] -t "relay identifier"
 *
 * */
class CreateFileCommand(relay: Relay) extends CommandExecutor {


    override def execute(implicit args: Array[String]): Unit = {
        checkArgs(args)
        val path = args(0)
        val isDirectory = args.contains("-D")
        val target = argAfter("-t")
        relay.scheduleTask(CreateFileTask(target, path, isDirectory))
                .complete()
    }

    def checkArgs(implicit args: Array[String]): Unit = {
        if (args.length != 3 && args.length != 4)
            throw CommandException("args length expected is 3 or 4")
        checkArgsContains( "-t")
    }
}
