package fr.`override`.linkit.api.utils

import scala.collection.mutable.ListBuffer

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

    def applyAll(t: T): this.type = {
        consumers.foreach(_.apply(t))
        this
    }

}
