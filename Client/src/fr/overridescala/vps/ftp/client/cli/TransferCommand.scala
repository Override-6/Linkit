package fr.overridescala.vps.ftp.client.cli

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.TaskConcoctor
import fr.overridescala.vps.ftp.api.task.tasks.{DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription}
import fr.overridescala.vps.ftp.client.cli.CommandUtils._

/**
 * syntax : <p>
 * upload | download -s "source path" -t "target identifier" -d "destination path"
 * */
class TransferCommand(private val relay: Relay,
                      private val isDownload: Boolean) extends CommandExecutor {


    override def execute(args: Array[String]): Unit = {
        checkArgs(args)
        val target = argAfter(args, "-t")
        val sourcePath = argAfter(args, "-s")
        val destination = argAfter(args, "-d")
        val source =
            if (isDownload) relay.scheduleTask(FileInfoTask.concoct(target, sourcePath)).complete()
            else FileDescription.fromLocal(sourcePath)
        //abort execution if source could not be found.
        if (source == null)
            return

        val transferDescription = TransferDescription.builder()
                .setSource(source)
                .setTargetID(target)
                .setDestination(destination)
                .build()
        val concoctor: TransferDescription => TaskConcoctor[_] = if (isDownload) DownloadTask.concoct else UploadTask.concoct
        relay.scheduleTask(concoctor(transferDescription))
    }

    def checkArgs(args: Array[String]): Unit = {
        if (args.length != 5)
            throw new IllegalArgumentException("argument length must be 5")
        checkArgsContains(args, "-t", "-s", "-d")
    }
}
