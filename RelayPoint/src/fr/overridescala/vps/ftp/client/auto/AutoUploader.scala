package fr.overridescala.vps.ftp.client.auto

import java.nio.file.StandardWatchEventKinds._
import java.nio.file.{FileSystems, Files, Path, WatchService}

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.tasks.{DeleteFileTask, UploadTask}
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescriptionBuilder}

class AutoUploader(relay: Relay, path: Path, targetRelay: String, localPath: String, targetPath: String) extends Automation {


    def registerFolder(service: WatchService, path: Path): Unit = {
        path.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        println(s"registered folder $path")
        Files.list(path).forEach(subPath => {
            if (!Files.isDirectory(subPath))
                return
            registerFolder(service, subPath)
        })
    }

    override def start(): Unit = {
        val service = FileSystems.getDefault.newWatchService()
        registerFolder(service, path)

        var key = service.take()
        while (key != null) {
            key.pollEvents().forEach(event => {
                val dir = key.watchable().asInstanceOf[Path]
                val path = dir.resolve(event.context().asInstanceOf[Path])
                event.kind() match {
                    case ENTRY_CREATE | ENTRY_MODIFY => onUpdate(path)
                    case ENTRY_DELETE => onDelete(path)
                }
            })
            key.reset()
            key = service.take()
        }
    }

    def onDelete(affected: Path): Unit = {
        val affectedString = affected.toString
        val relativePath = affectedString.substring(localPath.length, affectedString.length)
        val targetFile = targetPath + relativePath
        relay.scheduleTask(DeleteFileTask(targetRelay, targetFile))
                .queue(null, Console.err.println)
    }

    def onUpdate(affected: Path): Unit = {
        val transfer = new TransferDescriptionBuilder {
            source = FileDescription.fromLocal(affected)
            destination = targetPath
            targetID = targetRelay
        }
        relay.scheduleTask(UploadTask(transfer)).queue(null, msg => cancel())
    }


}
