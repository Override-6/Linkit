package fr.overridescala.vps.ftp.api.task

import java.net.InetSocketAddress
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference

import fr.overridescala.vps.ftp.api.exceptions.TransferException

abstract class Task[T](private val handler: TasksHandler,
                       private val target: InetSocketAddress)
        extends TaskAction[T] with TaskExecutor {

    private val onSuccess: AtomicReference[T => Unit] = new AtomicReference[T => Unit]()
    private val onError: AtomicReference[String => Unit] = new AtomicReference[String => Unit]()
    private val sessionID = ThreadLocalRandom.current().nextInt()

    final override def queue(onSuccess: T => Unit = t => {}, onError: String => Unit = Console.err.println): Unit = {
        this.onSuccess.set(onSuccess)
        this.onError.set(onError)
        handler.register(this, sessionID, target, true)
    }

    final override def completeNow(): T = {
        handler.register(this, sessionID, target, true)
        val atomicResult = new AtomicReference[T]()
        val onSuccess: T => Unit = result => synchronized {
            notify()
            atomicResult.set(result)
        }
        val onError: String => Unit = msg => synchronized {
            new TransferException(msg + "\n").printStackTrace()
            notify()
        }
        this.onSuccess.set(onSuccess)
        this.onError.set(onError)
        synchronized {
            wait()
        }
        atomicResult.get()
    }

    def sendTaskInfo(): Unit

    protected def error(msg: String): Unit = {
        val onError = this.onError.get()
        if (onError != null)
            onError(msg)
    }

    protected def success(t: T): Unit = {
        val onSuccess = this.onSuccess.get()
        if (onSuccess != null)
            onSuccess(t)
    }

}
