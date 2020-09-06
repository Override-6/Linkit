package fr.overridescala.vps.ftp.api.task

trait TaskAchiever {

    val taskType: TaskType

    def achieve(): Unit

    def preAchieve(): Unit = {

    }

}
