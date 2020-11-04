package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx

import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player.{FxPlayer, LocalFxPlayer, OnlineFxPlayer}
import fr.overridescala.vps.ftp.`extension`.ppc.logic.{Game, Player}
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Implémentation de la classe abstraite Game, adapte le jeux pour que les informations soient perçus graphiquement.
 * les joueurs demmandés ne sont plus des Player mais des FxPlayer, car la surcouche graphique demmande certaines
 * options en plus de la part des joueurs.
 * */
class GameInterface(player1: FxPlayer, player2: FxPlayer) extends Game(player1, player2) {

    private val stage = new Stage()
    private var player1View: MoveView = _ //Les valeures seront attribuées plus tard...
    private var player2View: MoveView = _

    override def startGame(): Unit = {
        Platform.runLater(() => {
            displayInterface()
        })
        try {
            super.startGame()
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
    }

    /**
     *
     * */
    private def displayInterface(): Unit = {
        stage.setTitle(player1.getName + " VS " + player2.getName)
        stage.getIcons.add(GameResource.VersusIcon)

        val gameView = new GameView()

        player1.setMoveContainer(gameView.container1)
        player2.setMoveContainer(gameView.container2)

        player1View = player1.getMoveView
        player2View = player2.getMoveView

        val box = new HBox(player1View, gameView, player2View)
        box.setAlignment(Pos.CENTER)
        stage.setScene(new Scene(box))
        stage.show()
    }

    override def onEnd(winner: Player, loser: Player): Unit = {
        // ne rien faire pour laisser les scores s'afficher
    }

    override def afterRound(roundState: Int): Unit = {
        Platform.runLater(() => {
            /* Affiche un "Gagné !!!" sous le nom du joueur gagnant de la manche.
             */
            roundState match {
                case -1 => player2View.onRoundWin()
                case 1 => player1View.onRoundWin()
                case 0 => //on ne fait rien.
            }
        })
        //arrêt de l'execution pendant 3000 ms (3 secondes)
        Thread.sleep(3000)
        Platform.runLater(() => {
            //Retour à la normal
            player1View.nextRound()
            player2View.nextRound()
        })
    }


}

