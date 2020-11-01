package fr.overridescala.vps.ftp.`extension`.ppc.logic.cli.player

import fr.overridescala.vps.ftp.`extension`.ppc.logic.{MovePacket, MoveType, Player}
import fr.overridescala.vps.ftp.api.packet.PacketChannel

/**
 * Cette classe nécéssite un [[PacketChannel]], et qui se chargera de recevoir les packets.
 * */
//noinspection RemoveRedundantReturn
class OnlinePlayer(name: String, channel: PacketChannel) extends Player {

    override def getName: String = name

    override def play(): MoveType = {
        println(getName + " joue...")
        /*demmande au PacketChannel d'attendre jusqu'a ce que le joueur ait joué. le type de coup joué est représenté par un MovePacket
         */
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

    /**
     * à l'inverse de OnlinePlayer, un WrappedPlayer se charge de jouer, et d'envoyer le type de coup joué via le PacketChannel
     * */
    //noinspection RemoveRedundantReturn
    class WrappedPlayer protected[OnlinePlayer](player: Player,
                                                implicit val channel: PacketChannel) extends Player {

        override def getName: String = player.getName

        override def play(): MoveType = {
            val moveType = player.play() //on fait jouer le joueur
            channel.sendPacket(MovePacket(moveType)) //on envoie le MovePacket
            return moveType
        }

    }

}
