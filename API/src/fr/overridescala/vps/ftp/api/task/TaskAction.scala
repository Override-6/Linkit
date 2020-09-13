package fr.overridescala.vps.ftp.api.task

trait TaskAction[T] {

    def queueWithSuccess(onSuccess: T => Unit): Unit
    def queueWithError(onError: String => Unit): Unit
    def queue(onSuccess: T => Unit, onError: String => Unit): Unit

    def completeNow(): T

}
