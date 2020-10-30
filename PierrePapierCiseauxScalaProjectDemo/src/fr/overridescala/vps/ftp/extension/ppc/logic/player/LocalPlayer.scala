package fr.overridescala.vps.ftp.`extension`.ppc.logic.player

import fr.overridescala.vps.ftp.`extension`.controller.cli.InputConsole
import fr.overridescala.vps.ftp.`extension`.ppc.logic.MoveType


class LocalPlayer(val name: String) extends Player {

    def this() = {
        this({
            InputConsole.ask("Veuillez choisir un pseudo : ")
        })
    }

    private val CHOICES = Seq("pierre", "papier", "ciseaux")
    private val CHOICES_DISPLAY = "Pierre, Papier et Ciseaux"


    override def getName: String = name

    override def play(): MoveType = {
        val msg = s"veuillez choisir un nom entre $CHOICES_DISPLAY: "
        val chosenMoveName: String = InputConsole.ask(msg, CHOICES: _*)

        MoveType.valueOf(chosenMoveName.toUpperCase)
    }
}
