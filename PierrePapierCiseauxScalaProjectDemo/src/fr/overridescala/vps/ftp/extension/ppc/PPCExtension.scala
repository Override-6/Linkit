package fr.overridescala.vps.ftp.`extension`.ppc

import fr.overridescala.vps.ftp.`extension`.ppc.logic.{MovePacket, OnlineGameStarterTask}
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension

class PPCExtension(client: Relay) extends TaskExtension(client) {


    override def main(): Unit = {
        val completerHandler = client.taskCompleterHandler
        completerHandler.putCompleter("PPC", initPacket => new OnlineGameStarterTask.Completer)
        client.packetManager.registerIfAbsent(classOf[MovePacket], MovePacket.Factory)
    }

}