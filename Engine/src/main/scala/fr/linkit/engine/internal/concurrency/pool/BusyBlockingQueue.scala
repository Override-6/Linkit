/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.api.internal.system.log.AppLoggers

import java.util
import java.util.concurrent.{BlockingQueue, TimeUnit}
import scala.collection.mutable.ListBuffer

/**
 * This queue works like a FIFO queue, excepted that blocking operations are replaced with
 * 'busy operations'.
 *
 * @see [[AbstractWorkerPool]] for more details on the 'busy operations' (or called 'busy thread system' in the doc).
 * @param pool the pool that created this blocking queue, at which will be used by the queue to handle busy locks.
 * */
class BusyBlockingQueue[A] private[concurrency](pool: AbstractWorkerPool) extends BlockingQueue[A] {
    
    private val content    = new util.LinkedList[A]()
    private val controller = new SimpleWorkerController()
    
    override def add(e: A): Boolean = {
        if (e == null)
            throw new NullPointerException("attempted to add a null item.")
        content.synchronized {
            AppLoggers.Worker.trace(s"Adding $e in content $this (${System.identityHashCode(this)})")
            content.add(e)
        }
        controller.wakeupAnyTask()
        true
    }
    
    override def offer(e: A): Boolean = add(e)
    
    override def put(e: A): Unit = add(e)
    
    override def offer(e: A, timeout: Long, unit: TimeUnit): Boolean = add(e)
    
    override def remove(): A = content.synchronized {
        content.removeFirst()
    }
    
    override def poll(): A = content.synchronized {
        val x = content.pollFirst()
        /*if (x == null)
            throw new NullPointerException("poll returned null")*/
        x
    }
    
    @workerExecution
    override def take(): A = {
        AppLoggers.Worker.trace(s"Taking item in $this (${System.identityHashCode(this)})...")
        if (content.isEmpty) {
            controller.pauseTask() //will be released once the queue isn't empty anymore
            return poll()
        }
        AppLoggers.Worker.trace(s"Something has been added ! $this (${System.identityHashCode(this)})")
        if (content.isEmpty)
            throw new Error("Content can't be empty.")
        poll()
    }
    
    @workerExecution
    override def poll(timeout: Long, unit: TimeUnit): A = {
        val toWait = unit.toMillis(timeout)
        
        //the lock object will be notified if an object has been inserted in the list.
        if (content.isEmpty)
            controller.pauseTaskForAtLeast(toWait)
        //will return the current head or null if the list is empty
        poll()
    }
    
    override def element(): A = {
        content.element()
    }
    
    override def peek(): A = {
        content.peekFirst()
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
    
    override def drainTo(c: util.Collection[_ >: A]): Int = ??? //TODO
    
    override def drainTo(c: util.Collection[_ >: A], maxElements: Int): Int = ??? //TODO
    
    override def isEmpty: Boolean = content.isEmpty
    
    override def containsAll(c: util.Collection[_]): Boolean = content.containsAll(c)
    
    override def addAll(c: util.Collection[_ <: A]): Boolean = {
        c.forEach(add)
        !c.isEmpty
    }
    
    override def removeAll(c: util.Collection[_]): Boolean = {
        c.forEach(e => remove(e))
        !c.isEmpty
    }
    
    override def retainAll(c: util.Collection[_]): Boolean = {
        content.retainAll(c)
    }
    
    override def clear(): Unit = content.synchronized {
        content.clear()
    }
    
    override def toArray: Array[AnyRef] = {
        val buff = ListBuffer.empty[Any]
        content.forEach(e => buff.addOne(e))
        buff.toArray.asInstanceOf[Array[AnyRef]]
    }
    
    override def toString: String = toArray().mkString("ProvidedBlockingQueue(", ", ", ")")
    
    override def toArray[T](a: Array[T with Object]): Array[T with Object] = {
        toArray.asInstanceOf[Array[T with Object]]
    }
    
}
