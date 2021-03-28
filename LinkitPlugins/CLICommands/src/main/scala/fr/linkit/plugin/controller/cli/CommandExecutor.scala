package fr.linkit.plugin.controller.cli

trait CommandExecutor {

    def execute(implicit args: Array[String]): Unit

}
