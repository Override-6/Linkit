package fr.`override`.linkit.api.concurrency

import java.util
import java.util.concurrent.{BlockingQueue, TimeUnit}

import scala.collection.mutable.ListBuffer

class ProvidedBlockingDeque[A](pool: RelayWorkerThreadPool) extends BlockingQueue[A] {

    private val list = ListBuffer.empty[A]

    override def add(e: A): Boolean = {
        list += e
        true
    }

    override def offer(e: A): Boolean = add(e)

    override def put(e: A): Unit = add(e)

    override def offer(e: A, timeout: Long, unit: TimeUnit): Boolean = add(e)

    override def remove(): A = {
        if (list.isEmpty)
            throw new NoSuchElementException()
        val head = list.head
        list.remove(0)
        head
    }

    override def poll(): A = {
        if (list.isEmpty)
            return _
        val head = list.head
        list.remove(0)
        head
    }

    override def take(): A = {
        pool.provideWhileThenWait(list.isEmpty)
        val head = list.head
        list.remove(0)
        head
    }

    override def poll(timeout: Long, unit: TimeUnit): A = {
        val toWait = unit.toMillis(timeout)
        pool.provide(toWait)

        poll()
    }

    override def element(): A = {
        val head = list.head
        if (head == null)
            throw new NoSuchElementException
        head
    }

    override def peek(): A = {
        list.head
    }

    override def remove(o: Any): Boolean = list.remove(list.indexOf(o)) != null

    override def contains(o: Any): Boolean = list.contains(o)

    override def size(): Int = list.size

    override def iterator(): util.Iterator[A] = new util.Iterator[A] {
        private val iterator = list.iterator
        override def hasNext: Boolean = iterator.hasNext

        override def next(): A = iterator.next()
    }

    override def remainingCapacity(): Int = -1 //Provided queue does not have defined capacity

    override def drainTo(c: util.Collection[_ >: A]): Int = null

    override def drainTo(c: util.Collection[_ >: A], maxElements: Int): Int = null

    override def isEmpty: Boolean = list.isEmpty

    override def toArray: Array[AnyRef] = list.toArray[AnyRef]

    override def toArray[T](a: Array[T]): Array[T] = list.toArray[T]

    override def containsAll(c: util.Collection[_]): Boolean = list.containsSlice(c.toArray)

    override def addAll(c: util.Collection[_ <: A]): Boolean = {
        c.forEach(add)
        !c.isEmpty
    }

    override def removeAll(c: util.Collection[_]): Boolean = {
        c.forEach(e => remove(e))
        !c.isEmpty
    }

    override def retainAll(c: util.Collection[_]): Boolean = null

    override def clear(): Unit = list.clear()
}
