package fr.overridescala.vps.ftp.`extension`.debug.commands

import fr.overridescala.vps.ftp.`extension`.controller.cli.{CommandException, CommandExecutor}
import fr.overridescala.vps.ftp.`extension`.debug.SendMessageTask
import fr.overridescala.vps.ftp.api.Relay

class SendMessageCommand(relay: Relay) extends CommandExecutor {

    override def execute(implicit args: Array[String]): Unit = {
        if (args.length < 1)
            throw CommandException("usage : msg <target> [message]")
        val target = args(0)
        val message = args.slice(1, args.length).mkString(" ")
        relay.scheduleTask(new SendMessageTask(target, message))
                .queue(
                    onError => Console.err.println("impossible d'envoyer le message.")
                )
    }
}
