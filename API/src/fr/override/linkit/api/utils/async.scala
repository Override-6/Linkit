package fr.`override`.linkit.api.utils

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import scala.util.control.NonFatal

@deprecated
object async {

    private val queue: BlockingDeque[() => Unit] = new LinkedBlockingDeque()

    //FIXME
    //TODO
    private val worker = new Thread(() => {
        while (true) try {
            println("test")
            val action = queue.takeLast()
            println(action.apply())
            println("ecotest")
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
    })

    worker.setName("Relay Async Execution Worker")
    worker.start()

    def apply(action: () => Unit): Unit = {
        if (Thread.currentThread() eq worker)
            throw new IllegalAccessException("Illegal recursive async.apply call operation")
        queue.addFirst(action)
        println(queue)
    }
}
