package fr.overridescala.vps.ftp.`extension`.sync

import fr.overridescala.vps.ftp.`extension`.sync.tasks.RelayFolderUpdaterTask
import fr.overridescala.vps.ftp.api.Relay

import scala.collection.mutable

class RelaySynchronizer(relay: Relay) {

    private val relayFolders = mutable.Map.empty[String, RelayTaskFolder]

    def getFolderOf(relayID: String): RelayTaskFolder = {
        if (relayFolders.contains(relayID))
            return relayFolders(relayID)
        val relayFolder = relay.scheduleTask(new RelayFolderUpdaterTask(relayID))
                .complete()
        if (relayFolder == null)
            return null
        relayFolders.put(relayID, relayFolder)
        relayFolder
    }


}