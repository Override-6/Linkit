package fr.overridescala.vps.ftp.`extension`.sync.tasks

import fr.overridescala.vps.ftp.`extension`.sync.RelayTaskFolder
import fr.overridescala.vps.ftp.`extension`.sync.tasks.RelayFolderUpdaterTask.TYPE
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{DataPacket, ErrorPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}
import fr.overridescala.vps.ftp.api.utils.Utils

class RelayFolderUpdaterTask(targetId: String) extends Task[RelayTaskFolder](targetId) {

    override def initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, targetID)

    override def execute(): Unit = {
        val relayFolder = channel.nextPacket() match {
            case errorPacket: ErrorPacket =>
                error(errorPacket.errorMsg)
                null
            case dataPacket: DataPacket =>
                Utils.deserialize(dataPacket.content): RelayTaskFolder
        }
        success(relayFolder)
    }

}

object RelayFolderUpdaterTask {
    val TYPE = "RTFU"

    class Completer(relay: Relay) extends TaskExecutor {

        override def execute(): Unit = {
            val folder = RelayTaskFolder.fromLocal(relay)
            channel.sendPacket(DataPacket(Utils.serialize(folder)))
        }

    }

}
