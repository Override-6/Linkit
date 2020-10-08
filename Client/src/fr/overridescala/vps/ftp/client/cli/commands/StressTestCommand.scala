package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.tasks.StressTestTask
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor}

/**
 * use : stress [data_length]
 * @param relay the relay to schedule tasks
 */
class StressTestCommand(relay: Relay) extends CommandExecutor {


  override def execute(args: Array[String]): Unit = {
    checkArgs(args)
    val dataLength = args(0).toInt
    relay.scheduleTask(StressTestTask.concoct(dataLength))
      .complete()
  }

  def checkArgs(args: Array[String]): Unit = {
    if (args.length != 1)
      throw new CommandException("use : stress <data_length>")
  }
}
