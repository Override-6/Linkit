package fr.overridescala.vps.ftp.`extension`.ppc

import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameInterface
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player.LocalFxPlayer
import fr.overridescala.vps.ftp.`extension`.ppc.logic.{MovePacket, OnlineGameStarterTask}
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension
import javafx.application.Application
import javafx.stage.Stage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PPCExtension(client: Relay) extends TaskExtension(client) {


    override def main(): Unit = {
        val completerHandler = client.taskCompleterHandler
        completerHandler.putCompleter("PPC", initPacket => new OnlineGameStarterTask.Completer)
        client.packetManager.registerIfAbsent(classOf[MovePacket], MovePacket.Factory)
        Future {
            Application.launch(classOf[PPCExtension.App])
        }
    }


}

object PPCExtension {

    class App extends Application {
        override def start(stage: Stage): Unit = ()
    }

}