package fr.overridescala.vps.ftp.`extension`.ppc

import fr.overridescala.vps.ftp.`extension`.controller.cli.CommandManager
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameResource
import fr.overridescala.vps.ftp.`extension`.ppc.logic.{MovePacket, OnlineGameStarterTask}
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension
import javafx.application.Application
import javafx.stage.Stage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Classe qui va lancer l'extension.
 * Cette extension est le jeux du Pierre papier Ciseaux.
 * @param client le client sur le quel ce programme est installé.
 * */
class PPCExtension(client: Relay) extends TaskExtension(client) {


    /**
     * Methode main qui va lancer / initialiser tout ce qui est requis pour que le jeux puisse démarrer
     * */
    override def main(): Unit = {
        /*
         * Ajout des tâches dans le selecteur du client
         * et des différents types de packets
         */
        val completerHandler = client.taskCompleterHandler
        completerHandler.putCompleter("PPC", initPacket => new OnlineGameStarterTask.Completer)
        client.packetManager.registerIfAbsent(classOf[MovePacket], MovePacket.Factory)

        /*
        * Ajout de la commande "rps" qui permet de lancer le jeux
        * */
        val commandManager: CommandManager = client.properties.getProperty("command_manager")
        commandManager.register("rps", new StartRPSGameCommand(client))

        /*
        * démarrage de JavaFX.
        * */
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