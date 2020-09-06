package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.PacketChannel

trait TaskCompleterFactory {

    def getCompleter(channel: PacketChannel, taskType: TaskType, header: String, content: Array[Byte]): TaskAchiever

}
