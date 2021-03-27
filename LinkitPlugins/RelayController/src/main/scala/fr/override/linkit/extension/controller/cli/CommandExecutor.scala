package fr.`override`.linkit.extension.controller.cli

trait CommandExecutor {

    def execute(implicit args: Array[String]): Unit

}
