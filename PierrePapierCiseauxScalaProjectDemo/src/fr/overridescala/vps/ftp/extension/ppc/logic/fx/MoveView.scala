package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx

import fr.overridescala.vps.ftp.`extension`.ppc.logic.Player
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameView.MoveContainer
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player.FxPlayer
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.text.Font

class MoveView private(playerName: String) extends VBox(50) {


    private var score = 0
    private val headerText = new Label(s"$playerName - $score")

    def onRoundWin(): Unit = {
        score += 1
        headerText.setText(s"$playerName - ${score}pt Gagné !!!")
    }

    def nextRound(): Unit = {
        headerText.setText(s"$playerName - ${score}pt")
    }

    private def display(): Unit = {
        headerText.setFont(Font.font(20))
        headerText.setAlignment(Pos.CENTER)
        setAlignment(Pos.CENTER)
        getChildren.add(headerText)
    }

    display()

}

object MoveView {

    class ControllableMoveView private[MoveView](playerName: String,
                                                 controller: Controller) extends MoveView(playerName) {
        override def nextRound(): Unit = {
            super.nextRound()
            controller.nextRound()
        }

        getChildren.add(controller)

    }

    class RemoteMoveView private[MoveView](player: Player, moveContainer: MoveContainer) extends MoveView(player.getName) {

        override def nextRound(): Unit = {
            super.nextRound()
            moveContainer.reset()
        }

        getChildren.add(new ImageView(GameResource.UnknownIcon))
        setAlignment(Pos.TOP_CENTER)

    }

    def remote(player: Player, moveContainer: MoveContainer): MoveView = new RemoteMoveView(player, moveContainer)

    def controllable(player: FxPlayer, controller: Controller): MoveView = new ControllableMoveView(player.getName, controller)


}
