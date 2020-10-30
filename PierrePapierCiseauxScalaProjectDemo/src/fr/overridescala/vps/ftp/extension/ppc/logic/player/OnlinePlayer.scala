package fr.overridescala.vps.ftp.`extension`.ppc.logic.player

import fr.overridescala.vps.ftp.`extension`.ppc.logic.MoveType
import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.DataPacket

//noinspection RemoveRedundantReturn
class OnlinePlayer(name: String, channel: PacketChannel) extends Player {

    override def getName: String = name

    override def play(): MoveType = {
        println(getName + " joue...")
        val response = channel.nextPacketAsP(): DataPacket
        return MoveType.valueOf(response.header)
    }

}

object OnlinePlayer {

    def wrap(player: Player, channel: PacketChannel): WrappedPlayer = {
        if (player.isInstanceOf[OnlinePlayer]) {
            throw new IllegalArgumentException("can't wrap an OnlinePlayer")
        }
        new WrappedPlayer(player, channel)
    }

    //noinspection RemoveRedundantReturn
    class WrappedPlayer private[OnlinePlayer](player: Player,
                                              implicit val channel: PacketChannel) extends Player {

        override def getName: String = player.getName

        override def play(): MoveType = {
            val moveType = player.play()
            channel.sendPacket(DataPacket(moveType.toString))
            return moveType
        }

    }

}
