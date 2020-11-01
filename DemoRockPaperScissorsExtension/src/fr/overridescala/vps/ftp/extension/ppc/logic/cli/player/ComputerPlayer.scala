package fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player

import java.util.concurrent.ThreadLocalRandom

import fr.overridescala.vps.ftp.`extension`.ppc.logic.{MoveType, Player}

//noinspection RemoveRedundantReturn
class ComputerPlayer extends Player {

    private val randomizer = ThreadLocalRandom.current();

    /**
     * @return "ordinateur", nom déja défini
     * */
    override def getName: String = s"ordinateur"

    /**
     * Tire un coup aléatoire
     * */
    override def play(): MoveType = {
        val plays = MoveType.values()
        val moveTypeIndex = randomizer.nextInt(plays.length)
        return plays(moveTypeIndex)
    }
}