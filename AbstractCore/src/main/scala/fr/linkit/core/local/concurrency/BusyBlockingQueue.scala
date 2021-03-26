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

package fr.linkit.core.local.concurrency

import fr.linkit.api.local.concurrency.workerExecution
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.util
import java.util.concurrent.{BlockingQueue, TimeUnit}
import scala.collection.mutable.ListBuffer

/**
 * This queue works like a FIFO queue, excepted that blocking operations are replaced with
 * 'busy operations'.
 *
 * @see [[BusyWorkerPool]] for more details on the 'busy operations' (or called 'busy thread system' in the doc).
 * @param pool the pool that created this blocking queue, at which will be used by the queue to handle busy locks.
 * */
class BusyBlockingQueue[A] private[concurrency](pool: BusyWorkerPool) extends BlockingQueue[A] {

    private val content = ListBuffer.empty[A]
    private val lock = new Object

    override def add(e: A): Boolean = {
        content += e
        lock.synchronized {
            lock.notify()
        }
        true
    }

    override def offer(e: A): Boolean = add(e)

    override def put(e: A): Unit = add(e)

    override def offer(e: A, timeout: Long, unit: TimeUnit): Boolean = add(e)

    override def remove(): A = {
        if (content.isEmpty)
            throw new NoSuchElementException()

        val head = content.head
        content.remove(0)
        head
    }

    override def poll(): A = {
        if (content.isEmpty)
            return _: A

        val head = content.head
        content.remove(0)
        head
    }

    @workerExecution
    override def take(): A = {
        pool.executeRemainingTasks(lock, content.isEmpty) //will be released once the queue is empty
        poll()
    }

    @workerExecution
    override def poll(timeout: Long, unit: TimeUnit): A = {
        val toWait = unit.toMillis(timeout)
        var total: Long = 0
        var last = now()

        //the lock object will be notified if an object has been inserted in the list.
        pool.executeRemainingTasks(lock, {
            val n = now()
            total += n - last
            last = n
            total <= toWait
        })
        //will return the current head or null if the list is empty
        poll()
    }

    override def element(): A = {
        val head = content.head
        if (head == null)
            throw new NoSuchElementException
        head
    }

    override def peek(): A = {
        content.head
    }

    override def remove(o: Any): Boolean = content.remove(content.indexOf(o)) != null

    override def contains(o: Any): Boolean = content.contains(o)

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

    override def clear(): Unit = content.clear()

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
