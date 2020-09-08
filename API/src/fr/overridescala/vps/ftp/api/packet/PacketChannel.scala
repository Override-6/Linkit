package fr.overridescala.vps.ftp.api.packet

import java.net.InetSocketAddress

import fr.overridescala.vps.ftp.api.task.TaskType

trait PacketChannel {

    def sendPacket(taskType: TaskType, header: String, content: Array[Byte] = Array()): Unit

    def nextPacket(): TaskPacket

    val ownerAddress: InetSocketAddress

}
