package fr.`override`.linkit.api.concurrency

import java.lang.annotation.{ElementType, Target}

import scala.annotation.StaticAnnotation

/**
 * Specifies that this method or constructor must be executed by a relay worker thread pool
 * If the annotated code isn't running in the worker thread pool, some problem could occur. <br>
 * An annotated code must not cares about if it is not currently executed by a worker thread.
 *
 * @see [[RelayWorkerThreadPool]]
 * */
@Target(Array[ElementType](ElementType.CONSTRUCTOR, ElementType.METHOD))
class relayWorkerExecution extends StaticAnnotation {

}
