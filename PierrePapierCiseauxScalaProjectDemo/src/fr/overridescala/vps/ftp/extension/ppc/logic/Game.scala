package fr.overridescala.vps.ftp.`extension`.ppc.logic

import fr.overridescala.vps.ftp.`extension`.ppc.logic.player.Player

class Game(player1: Player, player2: Player) {

    private var firstPlayerScore = 0
    private var secondPlayerScore = 0

    private val SCORE_BOUND = 3


    def startGame(): Unit = {
        println("Début du jeux !")
        println(s"------- ${player1.getName} VS ${player2.getName} -------")
        while (firstPlayerScore < SCORE_BOUND && secondPlayerScore < SCORE_BOUND) {
            playOnce()
            displayScores()
        }
        val playerTuple =
            if (firstPlayerScore > secondPlayerScore) (player1, player2)
            else (player2, player1)
        val winner = playerTuple._1
        val loser = playerTuple._2
        println(s"${winner.getName} a gagné contre ${loser.getName} !")
    }

    private def playOnce(): Unit = {
        val firstPlayerMove = player1.play()
        println(player1.getName + s" a joué ${firstPlayerMove.getTranslate}")
        val secondPlayerMove = player2.play()
        println(player2.getName + s" a joué ${secondPlayerMove.getTranslate}")

        if (firstPlayerMove.winAgainst(secondPlayerMove))
            firstPlayerScore += 1
        if (secondPlayerMove.winAgainst(firstPlayerMove))
            secondPlayerScore += 1
    }

    private def displayScores(): Unit = {
        val firstPlayerName = player1.getName
        val secondPlayerName = player2.getName
        println(s"Scores : $firstPlayerName : $firstPlayerScore - $secondPlayerName : $secondPlayerScore")
    }


}