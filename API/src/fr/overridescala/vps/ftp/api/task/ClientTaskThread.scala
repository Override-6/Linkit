package fr.overridescala.vps.ftp.api.task

import java.io.Closeable
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

class ClientTaskThread() extends Thread with Closeable {

    @volatile private val queue: BlockingQueue[TaskTicket] = new ArrayBlockingQueue[TaskTicket](-1)
    @volatile private var open = false

    override def run(): Unit = {
        open = true
        while (open) {
            queue.take().start()
        }
    }

    override def start(): Unit = super.start()

    override def close(): Unit = {
        open = false
        interrupt()
    }

    def addTicket(ticket: TaskTicket): Unit = {
        queue.add(ticket)
    }
}
