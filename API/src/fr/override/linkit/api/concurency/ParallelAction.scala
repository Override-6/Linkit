package fr.`override`.linkit.api.concurency

trait ParallelAction[A] {

    def workingThread: RelayWorkerThread

    def launchingThread: Thread

    def thenDo[B](success: => B): ParallelAction[B]

    def thenDo[B](success: A => B): ParallelAction[B]

    def onFail(callback: Throwable => Unit): this.type

}

object ParallelAction {
    def newHandler: ParallelActionService = new ParallelActionService

    class ParallelActionService {

    }

}
