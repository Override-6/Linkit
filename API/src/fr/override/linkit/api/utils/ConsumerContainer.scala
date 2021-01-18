package fr.`override`.linkit.api.utils

import fr.`override`.linkit.api.concurency.AsyncExecutionContext

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.control.NonFatal

case class ConsumerContainer[T]() {

    private val consumers = ListBuffer.empty[T => Unit]

    def add(consumer: T => Unit): this.type = {
        consumers += consumer
        this
    }

    /**
     * alias for [[add()]]
     * */
    def +=(consumer: T => Unit): this.type = add(consumer)

    def applyAllAsync(t: T, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        Future {
            applyAll(t, onException)
        }(AsyncExecutionContext)
        this
    }

    def applyAll(t: T, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        consumers.clone().foreach(consumer => {
            try {
                consumer.apply(t)
            } catch {
                case NonFatal(e) => onException(e)
            }
        })
        this
    }

}
