package fr.overridescala.vps.ftp.`extension`.sync

import fr.overridescala.vps.ftp.`extension`.sync.tasks.RelayFolderUpdaterTask
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension

class SyncExtension(relay: Relay) extends TaskExtension(relay) {

    override def main(): Unit = {
        val completerHandler = relay.taskCompleterHandler
        completerHandler.putCompleter(RelayFolderUpdaterTask.TYPE, _ => new RelayFolderUpdaterTask.Completer(relay))
    }

}
