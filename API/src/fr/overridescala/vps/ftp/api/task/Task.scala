package fr.overridescala.vps.ftp.api.task

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

import fr.overridescala.vps.ftp.api.exceptions.TransferException

abstract class Task[T](handler: TasksHandler, owner: InetSocketAddress)
        extends TaskAction[T] with TaskAchiever {

    private var onSuccess: Consumer[T] = _
    private var onError: Consumer[String] = _

    override def queueWithSuccess(onSuccess: Consumer[T]): Unit = {
        this.onSuccess = onSuccess
        handler.register(this, owner, true)
    }

    override def queueWithError(onError: Consumer[String]): Unit = {
        this.onError = onError
        handler.register(this, owner, true)
    }

    override def queue(onSuccess: Consumer[T], onError: Consumer[String]): Unit = {
        this.onSuccess = onSuccess
        this.onError = onError
        handler.register(this, owner, true)
    }

    override def completeNow(): T = {
        handler.register(this, owner, true)
        val atomicResult = new AtomicReference[T]()
        onSuccess = result => synchronized {
            notify()
            atomicResult.set(result)
        }
        onError = msg => synchronized {
            notify()
            throw new TransferException(msg)
        }
        synchronized {
            wait()
        }
        atomicResult.get()
    }

    protected def error(msg: String): Unit = {
        if (onError != null)
            onError.accept(msg)
    }

    protected def success(t: T): Unit = {
        if (onSuccess != null)
            onSuccess.accept(t)
    }

}
