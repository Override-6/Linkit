package fr.overridescala.vps.ftp.`extension`.controller.cli

trait CommandExecutor {

    def execute(implicit args: Array[String]): Unit

}
