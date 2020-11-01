package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player

import fr.overridescala.vps.ftp.`extension`.ppc.logic.Player
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameView.MoveContainer
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.MoveView

trait FxPlayer extends Player {

    def setMoveContainer(moveContainer: MoveContainer): Unit

    def getMoveView(): MoveView

}
