package fr.overridescala.vps.ftp.`extension`.ppc.logic

/**
 * trait (interface) qui permet à la classe [[Game]] de travailler avec plusieurs type de joueurs
 *
 * @see [[fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player.LocalPlayer]]
 * @see [[fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player.OnlinePlayer]]
 * @see [[fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player.ComputerPlayer]]
 * @see [[fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player.LocalPlayer]]
 * @see [[fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player.OnlineFxPlayer]]
 * @see [[fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player.LocalFxPlayer]]
 * */
trait Player {

    /**
     * @return le nom / pseudo du joueur
     * */
    def getName: String

    /**
     * permet au joueur d'effectuer un coup
     * @return le type de coup joué
     * */
    def play(): MoveType

}
