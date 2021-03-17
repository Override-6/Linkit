package fr.`override`.linkit.skull.internal.concurrency

trait Procrastinator {

    def runLater(task: => Unit): Unit

}
