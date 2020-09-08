package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.{PacketChannel, TaskPacket}

trait TaskCompleterFactory {

    def getCompleter(channel: PacketChannel, initTaskPacket: TaskPacket): TaskAchiever

}
