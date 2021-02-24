package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.system.CloseReason.NOT_SPECIFIED

trait JustifiedCloseable extends AutoCloseable {

    def close(reason: CloseReason): Unit

    def isClosed: Boolean

    def isOpen: Boolean = !isClosed

    override def close(): Unit = close(NOT_SPECIFIED)

}
