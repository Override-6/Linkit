package fr.`override`.linkit.api.local.concurrency

trait Procrastinator {

    def runLater(task: => Unit): Unit

}
