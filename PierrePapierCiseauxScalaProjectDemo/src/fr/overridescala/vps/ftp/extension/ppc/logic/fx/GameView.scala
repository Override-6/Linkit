package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx

import fr.overridescala.vps.ftp.`extension`.ppc.logic.MoveType
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameView.MoveContainer
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.{HBox, VBox}

class GameView extends VBox {

    val container1: MoveContainer = new MoveContainer(false)
    val container2: MoveContainer = new MoveContainer(true)

    private def display(): Unit = {
        setAlignment(Pos.CENTER)
        val versus = new ImageView(GameResource.VersusIcon)

        val hBox = new HBox(25, container1, versus, container2)
        hBox.setAlignment(Pos.CENTER)
        getChildren.add(hBox)
    }

    display()


}

object GameView {

    class MoveContainer(rotate: Boolean) extends HBox(50) {

        private val arrowImage = new ImageView(GameResource.ArrowOff)
        private val playedMoveImage = new ImageView(GameResource.NothingIcon)
        @volatile private var currentMove: MoveType = null

        def setMove(move: MoveType): Unit = synchronized {
            if (move == null) {
                playedMoveImage.setImage(GameResource.NothingIcon)
                return
            }
            notify()
            this.currentMove = move
            playedMoveImage.setImage(move.getIcon)
            arrowImage.setImage(GameResource.ArrowOn)
        }

        def pollMove: MoveType = {
            val move = currentMove
            currentMove = null
            move
        }

        def reset(): Unit = {
            currentMove = null
            playedMoveImage.setImage(GameResource.NothingIcon)
            arrowImage.setImage(GameResource.ArrowOff)
        }

        /**
         * Arrêt des executions jusqu'a ce que le coup soit joué.
         * */
        def waitUntilNextMove(): Unit = synchronized {
            if (currentMove == null)
                wait()
        }

        private def display(): Unit = {
            setAlignment(Pos.CENTER)
            getChildren.add(arrowImage)
            if (rotate) {
                arrowImage.setScaleX(-1)
                getChildren.add(playedMoveImage)
                return
            }
            playedMoveImage.setScaleX(-1)
            getChildren.add(0, playedMoveImage)
        }

        display()
    }

}
