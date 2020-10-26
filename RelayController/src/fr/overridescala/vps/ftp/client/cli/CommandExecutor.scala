package fr.overridescala.vps.ftp.client.cli

trait CommandExecutor {

    def execute(implicit args: Array[String]): Unit

}
