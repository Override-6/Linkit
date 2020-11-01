package fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player

import fr.overridescala.vps.ftp.`extension`.ppc.logic.{MovePacket, MoveType, Player}
import fr.overridescala.vps.ftp.api.packet.PacketChannel

//noinspection RemoveRedundantReturn
class OnlinePlayer(name: String, channel: PacketChannel) extends Player {

    override def getName: String = name

    override def play(): MoveType = {
        println(getName + " joue...")
        val response = channel.nextPacketAsP(): MovePacket
        println(getName + s" a joué ${response.move} !")
        return response.move
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
    class WrappedPlayer protected[OnlinePlayer](player: Player,
                                              implicit val channel: PacketChannel) extends Player {

        override def getName: String = player.getName

        override def play(): MoveType = {
            val moveType = player.play()
            channel.sendPacket(MovePacket(moveType))
            return moveType
        }

    }

}
