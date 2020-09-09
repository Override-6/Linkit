package fr.overridescala.vps.ftp.api.task

trait TaskExecutor {

    def execute(): Unit

    def sendTaskInfo(): Unit = {}

}
