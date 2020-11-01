package fr.overridescala.vps.ftp.`extension`.ppc.logic

import fr.overridescala.vps.ftp.`extension`.controller.cli.InputConsole
import fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.GameCli
import fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player.{ComputerPlayer, LocalPlayer}
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameView.MoveContainer
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player.LocalFxPlayer
import fr.overridescala.vps.ftp.api.Relay

//noinspection RemoveRedundantReturn
class GameConfig(relay: Relay) {

    private var displayInterface = false

    def displayConfigMenuThenStartGame(): Unit = {
        println("Configuration du jeux Pierre Papier Ciseaux !")
        displayInterface = InputConsole.ask("Voulez-vous ouvrir une interface ?", "oui", "non") == "oui"

        //forcer le jeux en ligne si une interface est demmandé
        var choice = ""
        if (displayInterface) choice = "oui"
        else choice = InputConsole.ask("Voulez-vous jouer en ligne ?", "oui", "non")

        if (choice == "oui")
            startOnlineGame()
        else startLocalGame()
    }


    def startLocalGame(): Unit = {
        val player1 = askPlayerInstance()
        val player2 = askPlayerInstance()
        new GameCli(player1, player2).startGame()
    }

    def startOnlineGame(): Unit = {
        val target = InputConsole.ask("Choisissez le nom de l'ordi au quel se connecter")
        if (target == relay.identifier) {
            Console.err.println("Vous ne pouvez pas jouer contre vous-même !")
        }

        val game = relay.scheduleTask(new OnlineGameStarterTask(target, displayInterface))
                .complete()
        if (game != null) {
            game.startGame()
        }
    }


    private def askPlayerInstance(): Player = {
        val question = "Choisissez un type de joueur (Local ou ordi)"
        val playerType = InputConsole.ask(question, "humain", "ordi")
        return playerType.toLowerCase() match {
            case "humain" => new LocalPlayer
            case "ordi" => new ComputerPlayer
        }
    }

}
