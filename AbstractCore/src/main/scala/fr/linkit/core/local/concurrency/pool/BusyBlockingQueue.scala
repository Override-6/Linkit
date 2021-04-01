/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.{JNullAssistant, now}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import java.util
import java.util.concurrent.{BlockingQueue, TimeUnit}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * This queue works like a FIFO queue, excepted that blocking operations are replaced with
 * 'busy operations'.
 *
 * @see [[BusyWorkerPool]] for more details on the 'busy operations' (or called 'busy thread system' in the doc).
 * @param pool the pool that created this blocking queue, at which will be used by the queue to handle busy locks.
 * */
class BusyBlockingQueue[A] private[concurrency](pool: BusyWorkerPool) extends BlockingQueue[A] {
    private val content     = ListBuffer.empty[A]
    private val entertainer = new WorkerEntertainer(pool)

    override def add(e: A): Boolean = {
        content += e
        AppLogger.error(s"Added ${e} in content $content")
        entertainer.stopFirstThreadAmusement()
        true
    }

    override def offer(e: A): Boolean = add(e)

    override def put(e: A): Unit = add(e)

    override def offer(e: A, timeout: Long, unit: TimeUnit): Boolean = add(e)

    override def remove(): A = content.synchronized {
        val head = content.headOption
        if (head.isEmpty)
            throw new NoSuchElementException()

        content.remove(0)
        head.get
    }

    override def poll(): A = {
        AppLogger.warn(content)
        val head = content.headOption
        if (head.isDefined) {
            content.remove(0)
            return head.get
        }
        JNullAssistant.getNull
    }

    @workerExecution
    override def take(): A = {
        AppLogger.error(s"$currentTasksId <> Taking item in $this")
        if (content.isEmpty)
            entertainer.amuseCurrentThread() //will be released once the queue isn't empty anymore
        poll()
    }

    @workerExecution
    override def poll(timeout: Long, unit: TimeUnit): A = {
        val toWait = unit.toMillis(timeout)

        //the lock object will be notified if an object has been inserted in the list.
        if (content.isEmpty)
            entertainer.amuseCurrentThreadFor(toWait)
        //will return the current head or null if the list is empty
        poll()
    }

    override def element(): A = {
        val head = content.headOption
        if (head.isEmpty)
            throw new NoSuchElementException
        head.get
    }

    override def peek(): A = {
        val head = content.headOption
        if (head.isDefined) {
            return head.get
        }
        JNullAssistant.getNull
    }

    override def remove(o: Any): Boolean = content.synchronized {
        content.remove(content.indexOf(o)) != null
    }

    override def contains(o: Any): Boolean = {
        content.contains(o)
    }

    override def size(): Int = content.size

    override def iterator(): util.Iterator[A] = new util.Iterator[A] {
        private val iterator = content.iterator

        override def hasNext: Boolean = iterator.hasNext

        override def next(): A = iterator.next()
    }

    override def remainingCapacity(): Int = -1 //Provided queue does not have defined capacity

    override def drainTo(c: util.Collection[_ >: A]): Int = throw new NotImplementedException() //TODO

    override def drainTo(c: util.Collection[_ >: A], maxElements: Int): Int = throw new NotImplementedException() //TODO

    override def isEmpty: Boolean = content.isEmpty

    override def containsAll(c: util.Collection[_]): Boolean = content.containsSlice(c.toArray)

    override def addAll(c: util.Collection[_ <: A]): Boolean = {
        c.forEach(add)
        !c.isEmpty
    }

    override def removeAll(c: util.Collection[_]): Boolean = {
        c.forEach(e => remove(e))
        !c.isEmpty
    }

    override def retainAll(c: util.Collection[_]): Boolean = {
        content.filterInPlace(c.contains)
        true
    }

    override def clear(): Unit = content.synchronized {
        content.clear()
    }

    override def toArray: Array[AnyRef] = {
        val buff = ListBuffer.empty[Any]
        content.foreach(e => buff.addOne(e))
        buff.toArray.asInstanceOf[Array[AnyRef]]
    }

    override def toString: String = content.toArray[Any].mkString("ProvidedBlockingQueue(", ", ", ")")

    override def toArray[T](a: Array[T with Object]): Array[T with Object] = {
        toArray.asInstanceOf[Array[T with Object]]
    }

}
