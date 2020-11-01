package fr.overridescala.vps.ftp.`extension`.ppc.logic.cli

import fr.overridescala.vps.ftp.`extension`.ppc.logic.{Game, Player}

/**
 * Implémentation de la classe abstraite Game, adapte le jeux pour que les informations soient perçu via la console.
 * */
class GameCli(player1: Player, player2: Player) extends Game(player1, player2) {

    override def startGame(): Unit = {
        println("Le jeux commence !")
        println(s"------- ${player1.getName} VS ${player2.getName} -------")
        super.startGame()
    }

    override def onEnd(winner: Player, loser: Player): Unit =
        println(s"${winner.getName} a gagné contre ${loser.getName} !")

    override def afterRound(state: Int): Unit = {
        state match {
            case -1 => println(player1.getName + " a gagné cette manche !")
            case 0 => println("égalité !")
            case 1 => println(player2.getName + " a gagné cette manche !")
        }
        println(s"${player1.getName}: $player1Score, ${player2.getName}: $player2Score")
    }
}
