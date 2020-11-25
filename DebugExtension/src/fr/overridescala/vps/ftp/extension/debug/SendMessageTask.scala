package fr.overridescala.vps.ftp.`extension`.debug

import fr.overridescala.vps.ftp.`extension`.debug.SendMessageTask.Type
import fr.overridescala.vps.ftp.api.packet.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}

class SendMessageTask(targetID: String, message: String) extends Task[Unit](targetID) {

    override def initInfo: TaskInitInfo = TaskInitInfo.of(Type, targetID, message)

    override def execute(): Unit = println(s"message '$message' has been sent to $targetID")

}

object SendMessageTask {

    val Type = "MSG"

    case class Completer(init: TaskInitPacket) extends TaskExecutor {
        val message = new String(init.content)

        override def execute(): Unit = {
            if (message.isEmpty)
                println(s"${channel.connectedID} sent you an empty message")
            else println(s"${channel.connectedID} : $message")
        }
    }

}
