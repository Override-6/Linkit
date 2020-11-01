package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx

import fr.overridescala.vps.ftp.`extension`.ppc.logic.MoveType
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameView.MoveContainer
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.{HBox, VBox}

class Controller(moveContainer: MoveContainer) extends VBox(50) {


    private val rockButton = createMoveButton(MoveType.ROCK)
    private val paperButton = createMoveButton(MoveType.PAPER)
    private val scissorsButton = createMoveButton(MoveType.SCISSORS)

    private val selector = new MoveSelector
    private val validator = new ImageView(GameResource.Validate)

    private var alreadyPlayed = false

    def nextRound(): Unit = {
        alreadyPlayed = false
        moveContainer.reset()
    }

    private def createMoveButton(moveType: MoveType): ImageView = {
        val texture = moveType.getTexture
        texture.setOnMouseClicked(_ => selector.update(moveType))
        texture.setFitHeight(75)
        texture.setFitWidth(75)
        texture
    }


    private def display(): Unit = {
        selector.setFitWidth(50)
        selector.setFitHeight(50)
        selector.setTranslateX(-25)
        selector.setTranslateY(25)
        validator.setFitHeight(100)
        validator.setFitWidth(100)
        validator.setOnMouseClicked(_ => {
            if (!alreadyPlayed)
                validateRoundMove()
        })
        val buttonBox = new HBox(30, rockButton, paperButton, scissorsButton, selector)
        setAlignment(Pos.BOTTOM_CENTER)
        getChildren.addAll(validator, buttonBox)
    }

    display()

    private def validateRoundMove(): Unit = {
        val move = selector.selectedMove
        if (move == null)
            return
        moveContainer.setMove(move)
        selector.reset()
        alreadyPlayed = true
    }

    class MoveSelector extends ImageView {
        @volatile private var moveType: MoveType = _

        def selectedMove: MoveType = moveType

        def update(move: MoveType): Unit = {
            this.moveType = move
            setImage(move.getTexture.getImage)
        }

        def reset(): Unit = {
            moveType = null
            setImage(GameResource.NothingIcon)
        }

        //default reset
        reset()
    }


}