package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.PacketChannel

trait TaskExecutor {

    def execute(channel: PacketChannel): Unit

    def sendTaskInfo(channel: PacketChannel): Unit = {}

}
