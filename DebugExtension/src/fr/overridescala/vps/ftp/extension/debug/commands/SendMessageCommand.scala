package fr.overridescala.vps.ftp.`extension`.debug.commands

import fr.overridescala.vps.ftp.`extension`.controller.cli.{CommandException, CommandExecutor}
import fr.overridescala.vps.ftp.api.Relay

class SendMessageCommand(relay: Relay) extends CommandExecutor {

    override def execute(implicit args: Array[String]): Unit = {
        if (args.length < 1)
            throw CommandException("usage : msg <target> [message]")
        val target = args(0)
        val message = args.slice(1, args.length).mkString(" ")

        val consoleOpt = relay.getConsoleOut(target)

        if (consoleOpt.isEmpty) {
            Console.err.println(s"Could not find remote console for '$target'")
            return
        }
        consoleOpt.get.println(message)

    }
}
