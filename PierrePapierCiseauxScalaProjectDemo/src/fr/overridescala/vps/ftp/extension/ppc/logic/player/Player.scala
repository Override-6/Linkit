package fr.overridescala.vps.ftp.`extension`.ppc.logic.player

import fr.overridescala.vps.ftp.`extension`.ppc.logic.MoveType

trait Player {

    def getName: String

    def play(): MoveType

}
