package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player

import fr.overridescala.vps.ftp.`extension`.controller.cli.InputConsole
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameView.MoveContainer
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.{Controller, MoveView}
import fr.overridescala.vps.ftp.`extension`.ppc.logic.{MoveType, Player}

class LocalFxPlayer(name: String) extends FxPlayer {

    def this() = this(InputConsole.ask("Choisissez un pseudo"))

    override def getName: String = name

    private var moveContainer: MoveContainer = null

    override def play(): MoveType = {
        println(s"$name is playing...")
        //L'execution continuera lorsque le coup sera validé par le joueur.
        moveContainer.waitUntilNextMove()
        val move = moveContainer.pollMove
        println(s"$name played $move !")
        move
    }

    override def setMoveContainer(moveContainer: MoveContainer): Unit = {
        this.moveContainer = moveContainer
    }

    override def getMoveView(): MoveView = MoveView.controllable(this, new Controller(moveContainer))
}
