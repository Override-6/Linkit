package fr.overridescala.vps.ftp.`extension`.ppc.logic

/**
 * Coueur du jeux.
 * Cette classe prend en charge deux joueurs, et les fait jouer les eun aprés les autres.
 * lorsque un joueur gagne, son score est incrémenter. si les deux joueurs ont joué le même coup ([[fr.overridescala.vps.ftp.`extension`.ppc.logic.MoveType]]
 * rien ne se passe.
 * le premier joueur qui obtient un score de 3 gagne.
 *
 * Cette classe est une classe abstraite, elle doit être implémenté pour pouvoir être instancié
 * */
abstract class Game(val player1: Player, val player2: Player) {

    @volatile protected var player1Score = 0
    @volatile protected var player2Score = 0

    private val SCORE_BOUND = 3

    /**
     * Démare le jeux (bloquant)
     * */
    def startGame(): Unit = {
        while (player1Score < SCORE_BOUND && player2Score < SCORE_BOUND) {
            val firstPlayerMove = player1.play()
            val secondPlayerMove = player2.play()

            var state = 0

            if (firstPlayerMove.winAgainst(secondPlayerMove)) {
                player1Score += 1
                state = 1
            }
            if (secondPlayerMove.winAgainst(firstPlayerMove)) {
                player2Score += 1
                state = -1
            }

            afterRound(state)
        }
        val playerTuple =
            if (player1Score > player2Score) (player1, player2)
            else (player2, player1)
        onEnd(playerTuple._1, playerTuple._2)
    }

    /**
     *  méthode abstraite.
     * */
    def onEnd(winner: Player, loser: Player): Unit

    /**
     * méthode abstraite appelée dés la fin d'un round
     * @param roundState si 1 -> player1 gagne
     *                   si -1 -> player2 gagne
     *                   si 0 -> égalité
     * */
    def afterRound(roundState: Int): Unit


}