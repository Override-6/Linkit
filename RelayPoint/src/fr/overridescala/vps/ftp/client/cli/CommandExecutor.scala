package fr.overridescala.vps.ftp.client.cli

trait CommandExecutor {

    def execute(args: Array[String]): Unit

}
