package fr.overridescala.vps.ftp.api.task

import java.util.function.Consumer

trait TaskAction[T] {

    def queueWithSuccess(onSuccess: Consumer[T]): Unit
    def queueWithError(onError: Consumer[String]): Unit
    def queue(onSuccess: Consumer[T], onError: Consumer[String]): Unit

    def completeNow(): T

}
