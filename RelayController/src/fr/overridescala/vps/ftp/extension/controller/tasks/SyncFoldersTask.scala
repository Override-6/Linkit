package fr.overridescala.vps.ftp.`extension`.controller.tasks

import java.util.concurrent.ThreadLocalRandom

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}
import SyncFoldersTask.{LOCAL_PATH_SEPARATOR, TYPE}
import fr.overridescala.vps.ftp.`extension`.controller.auto.AutomationManager
import fr.overridescala.vps.ftp.`extension`.controller.auto.sync.FolderSync

class SyncFoldersTask(relay: Relay, targetId: String, targetFolder: String, localFolder: String) extends Task[Unit](targetId) {

    override def initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, targetId, targetFolder ++ LOCAL_PATH_SEPARATOR ++ localFolder)

    override def execute(): Unit = {
        val automationManager: AutomationManager = relay.properties.getProperty("automation_manager")
        val channelID = ThreadLocalRandom.current().nextInt()
        channel.sendPacket(DataPacket("ChannelID", channelID.toString))
        val automation = new FolderSync(relay, targetId, localFolder, targetFolder, channelID)
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
            val response = channel.nextPacketAsP(): DataPacket
            val channelID = new String(response.content).toInt
            val automationManager: AutomationManager = relay.properties.getProperty("automation_manager")
            val automation = new FolderSync(relay, channel.connectedID, folder, senderFolder, channelID)
            automationManager.register(automation)
        }
    }

}
