package fr.overridescala.vps.ftp.`extension`.cloud.sync

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._

import com.sun.nio.file.{ExtendedWatchEventModifier, SensitivityWatchEventModifier}
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}
import fr.overridescala.vps.ftp.api.utils.Utils

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class FolderSync(localPath: String,
                 targetPath: String)(implicit channel: PacketChannel.Async) {

    private val ignoredPaths = ListBuffer.empty[Path]
    private val watchService = FileSystems.getDefault.newWatchService()
    private val listener = new FolderListener()(channel)

    channel onPacketReceived handlePacket

    def start(): Unit = {
        new Thread(() => {
            startWatchService()
        }).start()
    }

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

    private def filterEvents(events: ListBuffer[WatchEvent[Path]]): Unit = {
        val doNotFilter = events.filter(_.kind() == ENTRY_DELETE)
        val toFilter = events.filterNot(_.kind() == ENTRY_DELETE)
        val pathEvents = mutable.Map.empty[Path, WatchEvent[Path]]

        for (event <- toFilter) {
            val path = event.context()
            if (!pathEvents.contains(path))
                pathEvents.put(path, event)
        }
        events.clear()
        events ++= pathEvents.values ++= doNotFilter
    }

    private def dispatchEvents(key: WatchKey): Unit = {
        val events = key
                .pollEvents()
                .toArray(Array[WatchEvent[Path]]())
                .to(ListBuffer)

        val dir = key.watchable().asInstanceOf[Path]
        dispatchRenameEvents(dir, events)
        filterEvents(events)


        for (event <- events)
            dispatchEvent(event)

        def dispatchEvent(event: WatchEvent[Path]): Unit = {
            val context = event.context()
            val affected = dir.resolve(context)

            if (ignoredPaths.contains(affected)) {
                ignoredPaths -= affected
                println(s"IGNORED EVENT ${event.kind()} FOR $affected")
                return
            }
            println(s"DETECTED EVENT ${event.kind()} FOR $affected")

            event.kind() match {
                case ENTRY_CREATE => listener.onCreate(affected)
                case ENTRY_DELETE => listener.onDelete(affected)
                case ENTRY_MODIFY => listener.onModify(affected)
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
                listener.onRename(affected, newName)
                events -= event -= lastEvent
                return
            }
            lastEvent = event
        }

    }


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
                case e: FileSystemException =>
                    e.printStackTrace()
                    Files.setAttribute(path, "dos:readonly", false)
                    Files.deleteIfExists(path)
            }
        }
    }

    private def handlePacket(packet: Packet): Unit = {
        val syncPacket = packet.asInstanceOf[FolderSyncPacket]
        val order = syncPacket.order
        val path = toLocal(syncPacket.affectedPath)

        ignoredPaths += path
        order match {
            case "upload" => handleFileDownload(syncPacket)
            case "rename" => handleRenameOrder(syncPacket)
            case "delete" => deletePath(path)
            case "mkdirs" if Files.notExists(path) => Files.createDirectories(path)
        }
    }

    private def handleRenameOrder(syncPacket: FolderSyncPacket): Unit = {
        val newName = new String(syncPacket.content)
        val path = toLocal(syncPacket.affectedPath)

        val renamed = path.resolveSibling(newName)
        if (Files.exists(path))
            Files.move(path, renamed, StandardCopyOption.ATOMIC_MOVE)
    }

    private def handleFileDownload(syncPacket: FolderSyncPacket): Unit = {
        val path = toLocal(syncPacket.affectedPath)

        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent)
            if (path.toString.contains('.') && Files.notExists(path))
                Files.createFile(path)
        }
        if (Files.isWritable(path))
            Files.write(path, syncPacket.content, StandardOpenOption.CREATE)
    }

    private def toLocal(nonLocalPath: String): Path = {
        val relativePath = Utils.subPathOfUnknownFile(nonLocalPath, targetNameCount)
        Utils.formatPath(localPath + relativePath)
    }

    private val targetNameCount = targetPath.count(_ == File.separatorChar)

}