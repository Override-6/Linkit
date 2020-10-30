package fr.overridescala.vps.ftp.`extension`.controller.tasks

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension

class ClientExtension(relay: Relay) extends TaskExtension(relay) {


    override def main(): Unit = {
        val handler = relay.taskCompleterHandler
        handler.putCompleter(InitTaskCompleter.TYPE, _ => new InitTaskCompleter(relay))
    }

}