package fr.overridescala.vps.ftp.api.task

trait TaskAction[T] {

    def queue(onSuccess: T => Unit = t => {}, onError: String => Unit = Console.err.println): Unit

    def complete(): T

}
