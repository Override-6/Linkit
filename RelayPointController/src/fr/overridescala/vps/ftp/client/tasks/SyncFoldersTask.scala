package fr.overridescala.vps.ftp.client.tasks

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}
import fr.overridescala.vps.ftp.client.auto.{AutomationManager, FolderSync}
import fr.overridescala.vps.ftp.client.tasks.SyncFoldersTask.{LOCAL_PATH_SEPARATOR, TYPE}

//this task only send a packet to the target notifying that a folder have to be synchronized
class SyncFoldersTask(relay: Relay, targetId: String, targetFolder: String, localFolder: String) extends Task[Unit](targetId) {

    override def initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, targetId, targetFolder ++ LOCAL_PATH_SEPARATOR ++ localFolder)

    override def execute(): Unit = {
        val automationManager: AutomationManager = relay.properties.getProperty("automation_manager")
        val folderToLink = (channel.nextPacketAsP(): DataPacket).header
        val tempFolder = System.getProperty("java.io.tmpdir") + "/AutoSync/" + localFolder
        val automation = new FolderSync(relay, targetId, localFolder, tempFolder, folderToLink)
        automationManager.register(automation)
    }
}

object SyncFoldersTask {
    val TYPE = "SYNCF"
    private val LOCAL_PATH_SEPARATOR = "<local>"

    class Completer(relay: Relay, initPacket: TaskInitPacket) extends TaskExecutor {

        private val contentString = new String(initPacket.content)
        private val folderPathLength = contentString.indexOf(LOCAL_PATH_SEPARATOR)
        private val folder = contentString.substring(0, folderPathLength)
        private val senderFolder = contentString.substring(folderPathLength + LOCAL_PATH_SEPARATOR.length, contentString.length)

        override def execute(): Unit = {
            val automationManager: AutomationManager = relay.properties.getProperty("automation_manager")
            val tempFolder = System.getProperty("java.io.tmpdir") + "/AutoSync/" + folder
            val automation = new FolderSync(relay, channel.connectedIdentifier, folder, tempFolder, senderFolder)
            automationManager.register(automation)
            channel.sendPacket(DataPacket(tempFolder))
        }
    }

}
