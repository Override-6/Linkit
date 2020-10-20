package fr.overridescala.vps.ftp.client.auto

import java.nio.file.StandardWatchEventKinds._
import java.nio.file.{FileSystems, Files, Path, WatchService}

import fr.overridescala.vps.ftp.`extension`.fundamental.{DeleteFileTask, UploadTask}
import fr.overridescala.vps.ftp.`extension`.fundamental.transfer.TransferDescriptionBuilder
import fr.overridescala.vps.ftp.api.Relay

import scala.collection.mutable.ListBuffer

class FolderSync(relay: Relay, targetRelay: String, localPath: String, tempFolder: String, targetPath: String) extends Automation {

    override def start(): Unit = ???

    //TODO
    /*
    private val transferredPaths = ListBuffer.empty[String]

    override def start(): Unit = {
        listenLocalFolder()
    }

    def listenLocalFolder(): Unit = {
        val watcher = FileSystems.getDefault.newWatchService()
        registerFolder(watcher, Path.of(localPath))


        var key = watcher.take()
        while (key != null)  {
            key.pollEvents().forEach(event => {
                val dir = key.watchable().asInstanceOf[Path]
                val path = dir.resolve(event.context().asInstanceOf[Path])
                event.kind() match {
                    case ENTRY_CREATE | ENTRY_MODIFY => onUpdate(path)
                    case ENTRY_DELETE => onDelete(path)
                }
            })
            key.reset()
            key = watcher.take()
        }
    }

    def registerFolder(service: WatchService, path: Path): Unit = {
        path.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        println(s"registered folder $path")
        Files.list(path).forEach(subPath => {
            if (Files.isDirectory(subPath))
                registerFolder(service, subPath)
        })
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
            source = affected.toString
            destination = targetPath
            targetID = targetRelay
        }
        relay.scheduleTask(UploadTask(transfer)).queue(null, msg => cancel())
    }

 */
}

object FolderSync {

}
