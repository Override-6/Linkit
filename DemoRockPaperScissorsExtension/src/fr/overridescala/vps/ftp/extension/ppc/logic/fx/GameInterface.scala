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

class GameInterface(player1: FxPlayer, player2: FxPlayer) extends Game(player1, player2) {

    private val stage = new Stage()
    private var player1View: MoveView = null //Les valeures seront attribuées plus tard...
    private var player2View: MoveView = null

    override def startGame(): Unit = {
        Platform.runLater(() => {
            startGameInFxThread()
        })
    }

    private def startGameInFxThread(): Unit = {
        stage.setTitle(player1.getName + " VS " + player2.getName)
        stage.getIcons.add(GameResource.VersusIcon)

        val gameView = new GameView()

        player1.setMoveContainer(gameView.container1)
        player2.setMoveContainer(gameView.container2)

        player1View = player1.getMoveView()
        player2View = player2.getMoveView()

        val box = new HBox(player1View, gameView, player2View)
        box.setAlignment(Pos.CENTER)
        stage.setScene(new Scene(box))
        stage.show()
        Future {
            try {
                super.startGame()
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
        }
    }

    override def onEnd(winner: Player, loser: Player): Unit = {
        // ne rien faire pour laisser les scores s'afficher
    }

    override def afterRound(roundState: Int): Unit = {
        Platform.runLater(() => {
            roundState match {
                case -1 => player2View.onRoundWin()
                case 1 => player1View.onRoundWin()
                case 0 => //on ne fait rien.
            }
        })
        Thread.sleep(3000)
        Platform.runLater(() => {
            player1View.nextRound()
            player2View.nextRound()
        })
    }


}

