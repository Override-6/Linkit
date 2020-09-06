package fr.overridescala.vps.ftp.api.task

import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

import fr.overridescala.vps.ftp.api.exceptions.TransferException

abstract class Task[T]() {

    private var onSuccess: Consumer[T] = _
    private var onError: Consumer[String] = _

    val taskType: TaskType

    def queueWithSuccess(onSuccess: Consumer[T]): Task[T] = {
        this.onSuccess = onSuccess
        enqueue()
        this
    }

    def queueWithError(onError: Consumer[String]): Task[T] = {
        this.onError = onError
        enqueue()
        this
    }

    def queue(onSuccess: Consumer[T], onError: Consumer[String]): Task[T] = {
        this.onSuccess = onSuccess
        this.onError = onError
        enqueue()
        this
    }

    def complete(): T = {
        enqueue()
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

    def enqueue(): Unit

    protected def error(msg: String): Unit = {
        if (onError != null)
            onError.accept(msg)
    }

    protected def success(t: T): Unit = {
        if (onSuccess != null)
            onSuccess.accept(t)
    }

}
