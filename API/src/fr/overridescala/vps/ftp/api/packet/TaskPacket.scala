package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.task.TaskType

class TaskPacket(protected[api] val sessionID: Int,
                 val taskType: TaskType,
                 val header: String,
                 val content: Array[Byte] = Array()) {


    lazy val haveContent: Boolean = !content.isEmpty

    override def toString: String =
        s"TaskPacket{type: $taskType, header: $header, content: ${new String(content)}}"

}
