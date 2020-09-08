package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, DataPacket}

trait TaskCompleterFactory {

    def getCompleter(channel: PacketChannel, initTaskPacket: DataPacket): TaskAchiever

}
