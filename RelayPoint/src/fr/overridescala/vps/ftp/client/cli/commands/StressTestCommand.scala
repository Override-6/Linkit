package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.tasks.StressTestTask
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor}

class StressTestCommand(relay: Relay) extends CommandExecutor {


    override def execute(args: Array[String]): Unit = {
        checkArgs(args)
        val dataLength = args(0).toInt
        val isDownload = args(1).equalsIgnoreCase("-D")
        val async = args.contains("async")
        relay.scheduleTask(StressTestTask(dataLength, isDownload, async))
                .complete()
    }

    def checkArgs(args: Array[String]): Unit = {
        val argsLength = args.length
        if (argsLength != 2 && argsLength != 3)
            throw new CommandException(s"args length must be 2 or 3 ($argsLength)")
        if (!args(1).equals("-U") && !args(1).equals("-D"))
            throw new CommandException("args[1] must be -U or -D to spec upload or download test")
    }

}
