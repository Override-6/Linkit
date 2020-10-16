package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.`extension`.fundamental.{DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.Task
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescriptionBuilder}
import fr.overridescala.vps.ftp.client.cli.CommandUtils._
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor}

/**
 * syntax : <p>
 * upload | download -s "sourceP path" -t "target identifier" -d "destination path"
 * */
class TransferCommand private(private val relay: Relay,
                      private val isDownload: Boolean) extends CommandExecutor {


    override def execute(implicit args: Array[String]): Unit = {
        checkArgs(args)
        val target = argAfter("-t")
        val sourcePath = argAfter("-s")
        val dest = argAfter("-d")
        val sourceP =
            if (isDownload) relay.scheduleTask(FileInfoTask(target, sourcePath)).complete()
            else FileDescription.fromLocal(sourcePath)
        //abort execution if sourceP could not be found.
        if (sourceP == null)
            return

        val desc = new TransferDescriptionBuilder {
            source = sourceP
            targetID = target
            destination = dest
        }
        val task: Task[Unit] = if (isDownload) DownloadTask(desc) else UploadTask(desc)
        relay.scheduleTask(task)
                .complete()
    }

    def checkArgs(implicit args: Array[String]): Unit = {
        if (args.length != 6)
            throw new CommandException("argument length must be 6")
        checkArgsContains("-t", "-s", "-d")
    }



}

object TransferCommand {
    def download(relay: Relay): TransferCommand =
        new TransferCommand(relay, true)

    def upload(relay: Relay): TransferCommand =
        new TransferCommand(relay, false)
}
