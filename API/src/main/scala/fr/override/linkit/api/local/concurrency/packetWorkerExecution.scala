package fr.`override`.linkit.api.local.concurrency

import java.lang.annotation.{ElementType, Retention, RetentionPolicy, Target}

import scala.annotation.StaticAnnotation

/**
 * Specifies that this method or constructor must be executed by a packet worker thread
 * If the annotated code isn't running in the worker thread pool, some problem could occur. <br>
 *
 * @see [[IllegalThreadException]]
 * */
@Target(Array[ElementType](ElementType.CONSTRUCTOR, ElementType.METHOD))
@Retention(RetentionPolicy.CLASS)
class packetWorkerExecution extends StaticAnnotation {

}
