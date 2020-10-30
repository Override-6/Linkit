package fr.overridescala.vps.ftp.`extension`.ppc.logic

import fr.overridescala.vps.ftp.`extension`.controller.cli.InputConsole
import fr.overridescala.vps.ftp.`extension`.ppc.logic.player.{LocalPlayer, OnlinePlayer}
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}

class OnlineGameStarterTask(targetID: String) extends Task[Game](targetID) {

    setDoNotCloseChannel()

    override def initInfo: TaskInitInfo =
        TaskInitInfo.of("PPC", targetID)

    override def execute(): Unit = {
        val playerName = InputConsole.ask("Choisissez un pseudo : ")

        channel.sendPacket(DataPacket(playerName))

        println(s"Envoi de la requette à $targetID...")
        println("En attente de sa réponse...")
        val response = channel.nextPacketAsP(): DataPacket
        val remotePlayer = response.header match {
            case "oui" => new OnlinePlayer(response.contentAsString, channel)
            case "non" =>
                error(s"Le joueur distant n'a pas voulu jouer avec toi :c")
        }
        val localPlayer = OnlinePlayer.wrap(new LocalPlayer(playerName), channel)
        success(new Game(localPlayer, remotePlayer))
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

            val localPlayerName = InputConsole.ask("Choisissez votre pseudo : ")
            channel.sendPacket(DataPacket(choice, localPlayerName))

            val remotePlayer = OnlinePlayer.wrap(new LocalPlayer(localPlayerName), channel)
            val localPlayer = new OnlinePlayer(remotePlayerName, channel)

            new Game(localPlayer, remotePlayer).startGame()
        }
    }

}
