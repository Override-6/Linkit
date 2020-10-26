package fr.overridescala.vps.ftp.client.auto.sync

import java.nio.file.{Files, LinkOption, Path}
import java.util.concurrent.ThreadLocalRandom

import fr.overridescala.vps.ftp.client.auto.sync.FolderWatcher._
import org.jetbrains.annotations.Nullable

import scala.collection.mutable.ListBuffer

class FolderWatcher(folder: Path) {
    checkIsFolder(folder)

    @volatile private var lastState = new Folder(folder)
    private val listeners = ListBuffer.empty[PathEventListener]

    def start(step: Int): Unit = {
        while (true) {
            dispatchEvents()
            lastState = new Folder(folder)
            Thread.sleep(step)
        }
    }

    def register(pathEventListener: PathEventListener): Unit =
        listeners += pathEventListener

    private def dispatchEvents(): Unit = {

        println(lastState)
        Console.err.println(new Folder(folder))
        println()

        lastState.compareWithCurrent((kind, newName, affected) => {
            println(s"kind = $kind")
            println(s"affected = $affected")
            kind match {
                case CREATE => listeners.foreach(_.onCreate(affected))
                case DELETE => listeners.foreach(_.onDelete(affected))
                case MODIFY => listeners.foreach(_.onModify(affected))
                case RENAME => listeners.foreach(_.onRename(affected, newName))
            }
        })
    }


}

object FolderWatcher {

    val DELETE = "DELETE"
    val MODIFY = "MODIFY"
    val CREATE = "CREATE"
    val RENAME = "RENAME"

    val ID_FLAG = "user.identifier"

    private val randomizer = ThreadLocalRandom.current()

    class Folder(val path: Path) {
        checkIsFolder(path)

        val subDirectories: Array[Folder] = retrieveSubFolders(path)
        val files: Array[FolderItem] = retrieveFiles(path)

        def compareWithCurrent(onAnomaly: (String, String, Path) => Unit): Unit = {
            if (Files.notExists(path)) {
                onAnomaly(DELETE, null, path)
                return
            }
            files.foreach(_.compareWithCurrent(onAnomaly))

            //handling file creation :
            val currentSubPaths = retrieveFiles(path).map(_.path)
            val filesPath = files.map(_.path)
            for (path <- currentSubPaths) {
                if (!filesPath.contains(path))
                    onAnomaly(CREATE, null, path)
            }
            subDirectories.foreach(_.compareWithCurrent(onAnomaly))
        }

        override def toString: String = {
            toString0(0)
        }

        private def toString0(identLevel: Int): String = {
            val builder = new StringBuilder
            val length = files.length + subDirectories.length
            for (dir <- subDirectories) {
                builder.append(" ".repeat(identLevel))
                        .append("-|")
                        .append(dir.path)
                        .append(" [").append(length).append(" items]\n")
                        .append(dir.toString0(identLevel + 1))
            }
            for (file <- files) {
                builder.append(" ".repeat(identLevel))
                        .append('|')
                        .append(file)
                        .append('\n')
            }
            builder.toString()
        }

    }

    class FolderItem(val path: Path) {
        val isDir: Boolean = Files.isDirectory(path)
        val id: Any = getId(path)
        val size: Long = Files.size(path)

        /**
         * triggers for event DELETE, RENAME and MODIFY
         * @return true for any anomaly find
         * */
        def compareWithCurrent(onAnomaly: (String, String, Path) => Unit): Boolean = {
            if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS)) {
                val renamedPath = findPotentialRenamedPath(this)
                if (renamedPath == null) {
                    onAnomaly(DELETE, null, path)
                } else onAnomaly(RENAME, renamedPath.toFile.getName, path)
                return true
            }
            if (Files.size(path) != size) {
                onAnomaly(MODIFY, null, path)
                return true
            }
            false
        }

        override def toString: String = s"$path ($size b) id: $id"

    }

    @Nullable private def findPotentialRenamedPath(item: FolderItem): Path = {
        val currentPaths = Files.list(item.path.getParent)
        currentPaths.toArray(l => new Array[Path](l))
                .find(getId(_) == item.id)
                .orNull
    }

    private def retrieveFiles(path: Path): Array[FolderItem] = {
        Files.list(path)
                .filter(p => Files.exists(p) && !Files.isDirectory(p))
                .map(new FolderItem(_))
                .toArray(length => new Array(length))
    }


    private def getId(path: Path): Any = {

    }


    private def retrieveSubFolders(path: Path): Array[Folder] = {
        Files.list(path)
                .filter(p => Files.isDirectory(p))
                .map(p => new Folder(p))
                .toArray(length => new Array[Folder](length))
    }

    private def checkIsFolder(path: Path): Unit = {
        if (!Files.isDirectory(path))
            throw new IllegalArgumentException(s"$path is not a folder !")
    }

}





















