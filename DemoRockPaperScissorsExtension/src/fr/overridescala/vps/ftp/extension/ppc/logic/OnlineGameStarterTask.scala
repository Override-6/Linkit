package fr.overridescala.vps.ftp.`extension`.ppc.logic

import fr.overridescala.vps.ftp.`extension`.controller.cli.InputConsole
import fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.GameCli
import fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player.{LocalPlayer, OnlinePlayer}
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameInterface
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player.{LocalFxPlayer, OnlineFxPlayer}
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.util.FxPlatform
import fr.overridescala.vps.ftp.api.packet.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}
import javafx.application.Platform

/**
 * Tâche, qui va permettre au client ciblé d'accêpter ou non l'invitation, et de lancer le jeux entre les deux joueurs.
 * @param targetID l'id du client ciblé
 * @param displayInterface, n'est utilisé que localement, et détermine si le joueur qui a envoyé l'invitation joue avec une interface ou non.
 * */
class OnlineGameStarterTask(targetID: String, displayInterface: Boolean) extends Task[Game](targetID) {

    setDoNotCloseChannel()


    override def initInfo: TaskInitInfo =
        TaskInitInfo.of("PPC", targetID)

    override def execute(): Unit = {
        val playerName = InputConsole.ask("Choisissez un pseudo : ")

        channel.sendPacket(DataPacket(playerName))

        println(s"Envoi de la requette à $targetID...")
        println("En attente de sa réponse...")
        val response: DataPacket = channel.nextPacketAsP()
        val remotePlayerName = response.header match {
            case "oui" => response.contentAsString
            case "non" => error(s"Le joueur distant n'a pas voulu jouer avec toi :c")
        }
        conclude(playerName, remotePlayerName)
    }

    /**
     * Lance le jeux pour le premier joueur.
     * */
    def conclude(localPlayerName: String, remotePlayerName: String): Unit = {
        if (displayInterface) {
            val remotePlayer = new OnlineFxPlayer(remotePlayerName, channel)
            val localPlayer = OnlineFxPlayer.wrap(new LocalFxPlayer(localPlayerName), channel)
            FxPlatform.runOnThread(_ => {
                success(new GameInterface(localPlayer, remotePlayer))
            })
            return
        }
        val remotePlayer = new OnlinePlayer(remotePlayerName, channel)
        val localPlayer = OnlinePlayer.wrap(new LocalPlayer(localPlayerName), channel)
        success(new GameCli(localPlayer, remotePlayer))
    }

}

object OnlineGameStarterTask {

    class Completer() extends TaskExecutor {

        setDoNotCloseChannel()


        override def execute(): Unit = {
            val namePacket: DataPacket = channel.nextPacketAsP()
            val remotePlayerName = namePacket.header
            val msg = s"$remotePlayerName vous invite à jouer à Pierre Papier Ciseaux avec lui \n" +
                    "Répondez par 'oui' ou 'non' pour accepter ou refuser"
            val choice = InputConsole.ask(msg, "oui", "non")


            if (choice == "non") {
                println("Invitation refusée")
                channel.sendPacket(DataPacket(choice))
                return
            }

            val displayWindow = InputConsole.ask("Voulez-vous ouvrir une interface ?", "oui", "non") == "oui"

            val localPlayerName = InputConsole.ask("Choisissez votre pseudo : ")
            channel.sendPacket(DataPacket(choice, localPlayerName))
            startGame(displayWindow, localPlayerName, remotePlayerName)
        }

        /**
         * Lance le jeux pour le second joueur.
         * */
        def startGame(displayWindow: Boolean, localPlayerName: String, remotePlayerName: String): Unit = {
            if (displayWindow) {
                val remotePlayer = new OnlineFxPlayer(remotePlayerName, channel)
                val localPlayer = OnlineFxPlayer.wrap(new LocalFxPlayer(localPlayerName), channel)
                Platform.runLater(() => {
                    new GameInterface(localPlayer, remotePlayer).startGame()
                })
                return
            }
            val remotePlayer = new OnlinePlayer(remotePlayerName, channel)
            val localPlayer = OnlinePlayer.wrap(new LocalPlayer(localPlayerName), channel)
            new GameCli(localPlayer, remotePlayer).startGame()
        }

    }

}
