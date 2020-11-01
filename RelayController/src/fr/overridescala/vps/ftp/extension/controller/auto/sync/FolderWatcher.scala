package fr.overridescala.vps.ftp.`extension`.controller.auto.sync

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, LinkOption, Path}

import FolderWatcher._
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

        //println(lastState)
        //Console.err.println(new Folder(folder))
        //println()

        lastState.compareWithCurrent((kind, newName, affected) => {
            println(s"kind = $kind")
            kind match {
                case CREATE => listeners.foreach(_.onCreate(affected))
                case DELETE => listeners.foreach(_.onDelete(affected))
                case MODIFY => listeners.foreach(_.onModify(affected))
                case RENAME => listeners.foreach(_.onRename(affected, newName))
            }
        })
    }


}

private object FolderWatcher {

    val DELETE = "DELETE"
    val MODIFY = "MODIFY"
    val CREATE = "CREATE"
    val RENAME = "RENAME"

    class Folder(val path: Path) {
        checkIsFolder(path)

        val subDirectories: Array[Folder] = retrieveSubFolders(path)
        val files: Array[FolderItem] = retrieveItems(path)

        def compareWithCurrent(onAnomaly: (String, String, Path) => Unit): Unit = {
            if (Files.notExists(path)) {
                onAnomaly(DELETE, null, path)
                return
            }
            files.foreach(_.compareWithCurrent(onAnomaly))

            //handling file creation :
            val currentSubPaths = retrieveFiles(path)
            val filesPath = files.map(_.getPath)
            val renamedPaths = files.filter(_.lastEventKind == RENAME).map(_.getPath)

            for (path <- currentSubPaths) {
                if (!renamedPaths.contains(path) && !filesPath.contains(path))
                    onAnomaly(CREATE, null, path)
            }
            subDirectories.foreach(_.compareWithCurrent(onAnomaly))
        }

        override def toString: String = {
            toString0(0)
        }

        private def toString0(identLevel: Int): String = {
            val builder = new StringBuilder
            for (dir <- subDirectories) {
                val length = dir.files.length + dir.subDirectories.length
                builder.append(" " * identLevel)
                        .append("-|")
                        .append(dir.path)
                        .append(" [").append(length).append(" items]\n")
                        .append(dir.toString0(identLevel + 1))
            }
            for (file <- files) {
                builder.append(" " * identLevel)
                        .append('|')
                        .append(file)
                        .append('\n')
            }
            builder.toString()
        }

    }

    class FolderItem(private var path: Path) {
        val isDir: Boolean = Files.isDirectory(path)
        val id: Any = getId(path)
        val size: Long = Files.size(path)
        val name: String = path.getFileName.toString
        var lastEventKind: String = ""

        def getPath: Path = this.path

        /**
         * triggers for event DELETE, RENAME and MODIFY
         * @return true for any anomaly find
         * */
        def compareWithCurrent(onAnomaly: (String, String, Path) => Unit): Boolean = {
            if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS)) {
                val renamedPath = findPotentialRenamedPath(this)
                if (renamedPath == null) {
                    onAnomaly(DELETE, null, path)
                    lastEventKind = DELETE
                } else {
                    onAnomaly(RENAME, renamedPath.toFile.getName, path)
                    lastEventKind = RENAME
                    path = renamedPath
                }
                return true
            }
            if (Files.size(path) != size) {
                onAnomaly(MODIFY, null, path)
                lastEventKind = MODIFY
                return true
            }
            false
        }

        override def toString: String = s"$path ($size b) id: $id"

    }

    @Nullable private def findPotentialRenamedPath(item: FolderItem): Path = {
        val currentPaths = Files.list(item.getPath.getParent)
        currentPaths.toArray(l => new Array[Path](l))
                .find(getId(_) == item.id)
                .orNull
    }

    private def getId(path: Path): Any = {
        val attributes = Files.readAttributes(path, classOf[BasicFileAttributes])
        val key = attributes.fileKey()
        if (key != null)
            key
        else {
            val creation = attributes.creationTime().toMillis
            val lastModified = attributes.lastModifiedTime().toMillis
            val lastAccess = attributes.lastAccessTime().toMillis
            creation + (lastModified | lastAccess)
        }
    }

    private def retrieveFiles(path: Path): Array[Path] = {
        Files.list(path)
                .filter(p => !Files.isDirectory(p))
                .toArray(length => new Array(length))
    }

    private def retrieveItems(path: Path): Array[FolderItem] =
        retrieveFiles(path).map(new FolderItem(_))


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





















