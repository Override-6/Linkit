package fr.overridescala.vps.ftp.`extension`.cloud.filesync

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._

import com.sun.nio.file.{ExtendedWatchEventModifier, SensitivityWatchEventModifier}
import fr.overridescala.vps.ftp.api.packet.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}
import fr.overridescala.vps.ftp.api.utils.Utils

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

class FolderSync(localPath: String,
                 targetPath: String)(implicit channel: PacketChannel.Async) {

    private val ignoredPaths = ListBuffer.empty[Path]
    private val watchService = FileSystems.getDefault.newWatchService()

    channel onPacketReceived handlePacket

    new Thread(() => {
        startWatchService()
    }).start()

    private def startWatchService(): Unit = {
        val events = Array(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY): Array[WatchEvent.Kind[_]]
        Paths.get(localPath).register(watchService, events, ExtendedWatchEventModifier.FILE_TREE, SensitivityWatchEventModifier.HIGH)

        var key = watchService.take()
        while (key != null) {
            dispatchEvents(key)
            key.reset()
            key = watchService.take()
        }
    }


    def dispatchEvents(key: WatchKey): Unit = {
        val events = key
                .pollEvents()
                .toArray(Array[WatchEvent[Path]]())
                .to(ListBuffer)

        val dir = key.watchable().asInstanceOf[Path]
        dispatchRenameEvents(dir, events)

        for (event <- events)
            dispatchEvent(event)

        def dispatchEvent(event: WatchEvent[Path]): Unit = {
            val context = event.context()
            if (context == null)
                return
            val affected = dir.resolve(context)

            if (ignoredPaths.contains(affected)) {
                ignoredPaths -= affected
                return
            }
            println(s"DETECTED EVENT ${event.kind()} FOR $affected")

            event.kind() match {
                case ENTRY_CREATE => onCreate(affected)
                case ENTRY_DELETE => onDelete(affected)
                case ENTRY_MODIFY => onModify(affected)
            }
        }
    }

    def dispatchRenameEvents(dir: Path, events: ListBuffer[WatchEvent[Path]]): Unit = {
        if (events.length < 2)
            return
        var lastEvent: WatchEvent[Path] = events.head

        events.foreach(compareEvent)

        def compareEvent(event: WatchEvent[Path]): Unit = {
            val kind = event.kind()
            val lastKind = lastEvent.kind()

            if (lastKind == ENTRY_DELETE && kind == ENTRY_CREATE) {
                val newName = event.context().toString
                val affected = dir.resolve(lastEvent.context())
                onRename(affected, newName)
                events -= event -= lastEvent
                return
            }
            lastEvent = event
        }

    }

    private def onCreate(affected: Path): Unit = {
        if (Files.isDirectory(affected)) {
            channel.sendPacket(DataPacket("mkdirs", affected.toString))
            return
        }
        onModify(affected)
    }

    @tailrec
    private def onModify(affected: Path): Unit = {
        if (Files.isDirectory(affected) || Files.notExists(affected))
            return
        try {
            val in = Files.newInputStream(affected)
            val buff = new Array[Byte](Files.size(affected).toInt)
            val read = in.read(buff)
            val bytes = buff.slice(0, read)
            channel.sendPacket(DataPacket(s"SUPLOAD:$affected", bytes))
            in.close()
        } catch {
            case e: FileSystemException =>
                Console.err.println(e.getMessage)
                onModify(affected)
        }
    }

    private def onDelete(affected: Path): Unit = {
        println("DELETED " + affected)
        channel.sendPacket(DataPacket("delete", affected.toString))
    }

    private def onRename(affected: Path, newName: String): Unit = channel.sendPacket(DataPacket(s"rename>$newName", affected.toString))

    private def deletePath(path: Path): Unit = {
        if (Files.notExists(path))
            return
        deleteRecursively(path)

        def deleteRecursively(path: Path): Unit = {
            if (Files.isDirectory(path)) {
                Files.list(path).forEach(deleteRecursively)
            }
            try
                Files.deleteIfExists(path)
            catch {
                case e: FileSystemException => e.printStackTrace()
            }
        }
    }

    private def handlePacket(packet: Packet): Unit = {
        val data = packet.asInstanceOf[DataPacket]
        val order = data.header

        if (order.contains(':')) {
            handleComplexOrder(data)
            return
        }
        val path = toLocal(new String(data.content))
        if (order.startsWith("rename>") && Files.exists(path)) {
            val newName = order.split(">").last
            val renamed = path.resolveSibling(newName)
            ignoredPaths += renamed
            Files.move(path, renamed, StandardCopyOption.ATOMIC_MOVE)
            return
        }

        ignoredPaths += path
        order match {
            case "delete" => deletePath(path)
            case "mkdirs" if Files.notExists(path) => Files.createDirectories(path)
            case _ =>
        }
    }

    private def handleComplexOrder(data: DataPacket): Unit = {
        val order = data.header
        val affectedPath = order.split(":").last
        val path = toLocal(affectedPath)
        ignoredPaths += path

        if (order.startsWith("SUPLOAD")) {
            val out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)
            out.write(data.content)
            out.flush()
            out.close()
        }
    }

    private def toLocal(nonLocalPath: String): Path = {
        val relativePath = Utils.subPathOfUnknownFile(nonLocalPath, targetNameCount)
        Utils.formatPath(localPath + relativePath)
    }

    private val targetNameCount = targetPath.count(_ == File.separatorChar)

}
