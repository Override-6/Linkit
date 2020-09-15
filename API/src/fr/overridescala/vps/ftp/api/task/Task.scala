package fr.overridescala.vps.ftp.api.task

import java.net.InetSocketAddress
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference

import fr.overridescala.vps.ftp.api.exceptions.TransferException

abstract class Task[T](private val handler: TasksHandler,
                       private val target: InetSocketAddress)
        extends TaskAction[T] with TaskExecutor {

    private var onSuccess: T => Unit = _
    private var onError: String => Unit = Console.err.println
    private val sessionID = ThreadLocalRandom.current().nextInt()

    override def queueWithSuccess(consumer:T => Unit): Unit = {
        this.onSuccess = onSuccess
        handler.register(this, sessionID, target, true)
    }

    override def queueWithError(onError: String => Unit): Unit = {
        this.onError = onError
        handler.register(this, sessionID, target, true)
    }

    override def queue(onSuccess: T => Unit, onError: String => Unit): Unit = {
        this.onSuccess = onSuccess
        this.onError = onError
        handler.register(this, sessionID, target, true)
    }

    override def completeNow(): T = {
        handler.register(this, sessionID, target, true)
        val atomicResult = new AtomicReference[T]()
        onSuccess = result => synchronized {
            notify()
            atomicResult.set(result)
        }
        onError = msg => synchronized {
            new TransferException(msg + "\n").printStackTrace()
            notify()
        }
        synchronized {
            wait()
        }
        atomicResult.get()
    }

    protected def error(msg: String): Unit = {
        if (onError != null)
            onError(msg)
    }

    protected def success(t: T): Unit = {
        if (onSuccess != null)
            onSuccess(t)
    }

}
