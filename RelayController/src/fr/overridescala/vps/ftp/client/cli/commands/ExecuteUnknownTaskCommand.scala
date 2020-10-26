package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.EmptyPacket
import fr.overridescala.vps.ftp.api.task.{Task, TaskInitInfo}
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor}

class ExecuteUnknownTaskCommand(relay: Relay) extends CommandExecutor {


    override def execute(implicit args: Array[String]): Unit = {
        if (args.length != 2)
            throw new CommandException("use : exec <task_name> <target>")
        val taskName = args(0)
        val target = args(1)
        relay.scheduleTask(new UnknownTask(taskName, target))
                .queue(
                    onSuccess => println("success !"),
                    onError => Console.err.println("error :(")
                )
    }

    private class UnknownTask(name: String, target: String) extends Task[Unit](target) {

        override def initInfo: TaskInitInfo =
            TaskInitInfo.of(name, target)

        override def execute(): Unit = {
            channel.sendPacket(EmptyPacket())
        }
    }



}
