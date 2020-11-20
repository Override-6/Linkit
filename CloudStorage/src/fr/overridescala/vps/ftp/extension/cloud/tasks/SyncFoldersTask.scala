package fr.overridescala.vps.ftp.`extension`.cloud.tasks

import fr.overridescala.vps.ftp.`extension`.cloud.filesync.FolderSync
import fr.overridescala.vps.ftp.`extension`.cloud.tasks.SyncFoldersTask.{LOCAL_PATH_SEPARATOR, TYPE}
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.system.Reason
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}

class SyncFoldersTask(relay: Relay, targetId: String, targetFolder: String, localFolder: String) extends Task[Unit](targetId) {
    setDoNotCloseChannel()

    override def initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, targetId, targetFolder ++ LOCAL_PATH_SEPARATOR ++ localFolder)

    override def execute(): Unit = {
        channel.close(Reason.INTERNAL)
        val asyncChannel = relay.createAsyncChannel(targetId, channel.channelID)
        new FolderSync(localFolder, targetFolder)(asyncChannel)
    }
}

object SyncFoldersTask {
    val TYPE = "SYNCF"
    private val LOCAL_PATH_SEPARATOR = "<local>"

    class Completer(relay: Relay, initPacket: TaskInitPacket) extends TaskExecutor {
        setDoNotCloseChannel()

        private val contentString = new String(initPacket.content)
        private val folderPathLength = contentString.indexOf(LOCAL_PATH_SEPARATOR)
        private val localFolder = contentString.substring(0, folderPathLength)
        private val remoteFolder = contentString.substring(folderPathLength + LOCAL_PATH_SEPARATOR.length, contentString.length)

        override def execute(): Unit = {
            channel.close(Reason.INTERNAL)
            val asyncChannel = relay.createAsyncChannel(initPacket.senderID, channel.channelID)
            new FolderSync(localFolder, remoteFolder)(asyncChannel)
        }
    }

}
