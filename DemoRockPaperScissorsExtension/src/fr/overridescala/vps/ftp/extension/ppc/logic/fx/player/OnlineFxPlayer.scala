package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player

import fr.overridescala.vps.ftp.`extension`.ppc.logic.{MovePacket, MoveType, Player}
import fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player.OnlinePlayer
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.GameView.MoveContainer
import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.MoveView
import fr.overridescala.vps.ftp.api.packet.PacketChannel

/**
 * même comportement que [[OnlinePlayer]] mais avec la surcouche graphique
 * */
class OnlineFxPlayer(name: String, channel: PacketChannel.Sync) extends OnlinePlayer(name, channel) with FxPlayer {

    private var container: MoveContainer = _

    override def play(): MoveType = {
        val moveType = super.play()
        container.setMove(moveType)
        moveType
    }

    override def setMoveContainer(moveContainer: MoveContainer): Unit = container = moveContainer

    override def getMoveView: MoveView = MoveView.remote(this, container)
}

object OnlineFxPlayer {
    def wrap(localFxPlayer: LocalFxPlayer, channel: PacketChannel): FxPlayer = {
        new WrappedFxPlayer(localFxPlayer, channel)
    }

    /**
     * même comportement que [[OnlinePlayer.WrappedPlayer]] mais avec la surcouche graphique
     * */
    //noinspection RemoveRedundantReturn
    class WrappedFxPlayer private[OnlineFxPlayer](player: LocalFxPlayer,
                                                implicit val packetChannel: PacketChannel) extends OnlinePlayer.WrappedPlayer(player, packetChannel) with FxPlayer {

        override def getName: String = player.getName

        override def play(): MoveType = {
            val moveType = player.play()
            channel.sendPacket(MovePacket(moveType))
            return moveType
        }

        override def setMoveContainer(moveContainer: MoveContainer): Unit =
            player.setMoveContainer(moveContainer)

        override def getMoveView: MoveView = player.getMoveView
    }

}
