package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player

import fr.overridescala.vps.ftp.`extension`.ppc.logic.Player
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameView.MoveContainer
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.MoveView

/**
 * sous-interface de Player, qui rajoute des éléments pour permettre la gestion graphique.
 * */
trait FxPlayer extends Player {

    def setMoveContainer(moveContainer: MoveContainer): Unit

    def getMoveView: MoveView

}
