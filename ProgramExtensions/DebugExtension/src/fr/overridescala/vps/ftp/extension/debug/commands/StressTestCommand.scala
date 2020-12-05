package fr.overridescala.vps.ftp.`extension`.debug.commands

import fr.overridescala.vps.ftp.`extension`.controller.cli.{CommandException, CommandExecutor}
import fr.overridescala.vps.ftp.`extension`.debug.StressTestTask
import fr.overridescala.vps.ftp.api.Relay

class StressTestCommand(relay: Relay) extends CommandExecutor {


    override def execute(implicit args: Array[String]): Unit = {
        checkArgs(args)
        val dataLength = args(0).toInt
        val isDownload = args(1).equalsIgnoreCase("-D")
        relay.scheduleTask(StressTestTask(dataLength, isDownload))
                .complete()
    }

    def checkArgs(args: Array[String]): Unit = {
        val argsLength = args.length
        if (argsLength != 2 && argsLength != 3)
            throw CommandException(s"args length must be 2 or 3 ($argsLength)")
        if (!args(1).equals("-U") && !args(1).equals("-D"))
            throw CommandException("args[1] must be -U or -D to spec upload or download test")
    }

}
