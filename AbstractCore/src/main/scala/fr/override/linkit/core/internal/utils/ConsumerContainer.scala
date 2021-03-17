package fr.`override`.linkit.core.internal.utils

import fr.`override`.linkit.internal.concurrency.RelayThreadPool

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConsumerContainer[T]() {

    private val consumers = ListBuffer.empty[ConsumerExecutor]

    def add(consumer: T => Unit): this.type = {
        consumers += new ConsumerExecutor(consumer, false)
        this
    }

    /**
     * Will add the consumer in the list, and remove it once it was executed.
     *
     * @param consumer the action to perform
     * */
    def addOnce(consumer: T => Unit): this.type = {
        consumers += new ConsumerExecutor(consumer, true)
        this
    }

    def remove(consumer: T => Unit): this.type = {
        consumers.filterInPlace(_.isSameConsumer(consumer))
        this
    }

    def clear(): this.type = {
        consumers.clear()
        this
    }

    /**
     * alias for [[ConsumerContainer#add()]]
     * */
    def +=(consumer: T => Unit): this.type = add(consumer)

    /**
     * alias for [[ConsumerContainer#addOnce()]]
     * */
    def +:+=(consumer: T => Unit): this.type = addOnce(consumer)

    /**
     * alias for [[ConsumerContainer#remove()]]
     * */
    def -=(consumer: T => Unit): this.type = remove(consumer)

    def applyAllAsync(t: T, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        RelayThreadPool.runLaterOrHere {
            applyAll(t, onException)
        }
        this
    }

    def applyAll(t: T, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        consumers.indices.foreach(i => {
            try {
                val consumer = consumers(i)
                consumer.execute(t)
            } catch {
                case NonFatal(e) => onException(e)
            }
        })
        this
    }

    override def toString: String = s"ConsumerContainer($consumers)"

    private class ConsumerExecutor(consumer: T => Unit, executeOnce: Boolean) {
        def execute(t: T): Unit = {
            if (executeOnce) consumer.synchronized {
                //synchronise in order to be sure that another thread would not start to execute the
                //consumer again when the first thread is removing it from the queue.
                consumer(t)
                remove(consumer)
                return
            }
            consumer(t)
        }

        def isSameConsumer(consumer: T => Unit): Boolean = this.consumer == consumer
    }

}

object ConsumerContainer {
    def apply[T](): ConsumerContainer[T] = new ConsumerContainer()
}
