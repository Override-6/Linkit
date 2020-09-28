package fr.overridescala.vps.ftp.api.task

case class TaskInitInfo private (taskType: String, targetID: String, content: Array[Byte])

object TaskInitInfo {
    def of(taskType: String, targetID: String, content: Array[Byte] = Array()): TaskInitInfo = new TaskInitInfo(taskType, targetID, content)
}
