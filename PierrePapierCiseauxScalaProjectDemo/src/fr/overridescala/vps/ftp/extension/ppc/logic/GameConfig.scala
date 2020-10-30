package fr.overridescala.vps.ftp.`extension`.ppc.logic

import fr.overridescala.vps.ftp.`extension`.controller.cli.InputConsole
import fr.overridescala.vps.ftp.`extension`.ppc.logic.player.{ComputerPlayer, LocalPlayer, Player}
import fr.overridescala.vps.ftp.api.Relay

//noinspection RemoveRedundantReturn
class GameConfig(relay: Relay) {


    def displayConfigMenuThenStartGame(): Unit = {
        println("Configuration du jeux Pierre Papier Ciseaux !")
        val choice = InputConsole.ask("Voulez-vous jouer en ligne ?", "oui", "non")

        if (choice == "oui")
            startOnlineGame()
        else startLocalGame()
    }

    def startLocalGame(): Unit = {
        val player1 = askPlayerInstance()
        val player2 = askPlayerInstance()
        new Game(player1, player2).startGame()
    }

    def startOnlineGame(): Unit = {
        val target = InputConsole.ask("Choisissez le nom de l'ordi au quel se connecter")
        if (target == relay.identifier) {
            Console.err.println("Vous ne pouvez pas jouer contre vous-même !")
        }

        val game = relay.scheduleTask(new OnlineGameStarterTask(target))
                .complete()
        if (game != null)
            game.startGame()
    }

    def askPlayerInstance(): Player = {
        val question = "Choisissez un type de joueur (Local ou ordi)"
        val playerType = InputConsole.ask(question, "local", "ordi")
        return playerType.toLowerCase() match {
            case "local" => new LocalPlayer
            case "ordi" => new ComputerPlayer
        }
    }

}
