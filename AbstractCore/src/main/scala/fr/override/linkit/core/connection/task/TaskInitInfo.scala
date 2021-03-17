package fr.`override`.linkit.core.connection.task

case class TaskInitInfo private (taskType: String, targetID: String, content: Array[Byte])

object TaskInitInfo {
    def of(taskType: String, targetID: String, content: Array[Byte] = Array()): TaskInitInfo = new TaskInitInfo(taskType, targetID, content)
    def of(taskType: String, targetID: String, content: String): TaskInitInfo = of(taskType, targetID, content.getBytes)
}
