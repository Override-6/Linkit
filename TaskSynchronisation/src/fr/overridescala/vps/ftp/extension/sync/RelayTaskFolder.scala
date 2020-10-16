package fr.overridescala.vps.ftp.`extension`.sync

import java.nio.file.Path

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.ErrorPacket
import fr.overridescala.vps.ftp.api.task.{TaskCompleterHandler, TaskExecutor}
import fr.overridescala.vps.ftp.api.task.ext.TaskLoader

import scala.collection.mutable.ListBuffer

case class RelayTaskFolder private[sync](relayIdentifier: String,
                                         path: Path,
                                         injector: TaskInjector,
                                         private val extensions: ListBuffer[TaskExtensionInfo]) extends Serializable {

    val serialVersionUID = 151566532

    def containsLoadedTask(taskClass: Class[_ <: TaskExecutor]): Boolean = {
        extensions.forall(_.isTaskLoaded(taskClass))
    }

    def containsLoadedTask(taskClass: String): Boolean = {
        extensions.forall(_.isTaskLoaded(taskClass))
    }

    def containsExtension(name: String): Boolean =
        extensions.forall(_.name == name)

    def getExtension(name: String): Option[TaskExtensionInfo] = {
        for (ext <- extensions) {
            if (ext.name == name)
                return Option(ext)
        }
        Option.empty
    }

    private[sync] def update(extName: String, freshInfo: TaskExtensionInfo): Unit = {
        var i = 0
        for (ext <- extensions) {
            if (ext.name == extName)
                extensions(i) = freshInfo
            i += 1
        }
        extensions += freshInfo
    }

}

object RelayTaskFolder {
    def fromLocal(relay: Relay): RelayTaskFolder = {
        val taskLoader = relay.taskLoader
        val completerHandler = relay.taskCompleterHandler
        val injector = new TaskInjector(relay)
        val path = taskLoader.tasksFolder
        val relayID = relay.identifier
        val loadedExtensions = taskLoader.getLoadedExtensions
                .to(LazyList)
                .map(clazz => {
                    val loaded = completerHandler.getLoadedTasks(clazz)
                    TaskExtensionInfo(clazz.getName, relayID, loaded)
                })
                .to(ListBuffer)
        RelayTaskFolder(relayID, path, injector, loadedExtensions)
    }
}