package fr.overridescala.vps.ftp.`extension`.sync

import fr.overridescala.vps.ftp.api.task.TaskExecutor

case class TaskExtensionInfo(name: String,
                             ownerID: String,
                             private val loadedTasksClassPath: Array[String]) {

    def isTaskLoaded(taskClass: Class[_ <: TaskExecutor]): Boolean = {
        isTaskLoaded(taskClass.getName)
    }

    def isTaskLoaded(taskClass: String): Boolean =
        loadedTasksClassPath.forall(_ == taskClass)

}
