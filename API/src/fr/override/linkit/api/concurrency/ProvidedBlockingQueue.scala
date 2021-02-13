package fr.`override`.linkit.api.concurrency

import java.util
import java.util.concurrent.{BlockingQueue, TimeUnit}

import scala.collection.mutable.ListBuffer

class ProvidedBlockingQueue[A] private[concurrency](pool: RelayWorkerThreadPool) extends BlockingQueue[A] {

    private val list = ListBuffer.empty[A]
    private val lock = new Object

    override def add(e: A): Boolean = {
        list += e
        lock.synchronized {
            lock.notifyAll()
            //println("NOTIFIED :D")
        }
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
            return _: A
        val head = list.head
        list.remove(0)
        head
    }

    @relayWorkerExecution
    override def take(): A = {
        //println(s"PERFORMING TAKE ($list)")
        pool.provideAllWhileThenWait(lock, list.isEmpty)
        poll()
    }

    @relayWorkerExecution
    override def poll(timeout: Long, unit: TimeUnit): A = {
        val toWait = unit.toMillis(timeout)
        var total: Long = 0
        var last = now()

        //println(s"PERFORMING TIMED POLL ($list)")
        pool.provideAllWhileThenWait(lock, {
            val n = now()
            total += n - last
            last = n
            total <= toWait
        })

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

    override def drainTo(c: util.Collection[_ >: A]): Int = throw new UnsupportedOperationException()

    override def drainTo(c: util.Collection[_ >: A], maxElements: Int): Int = throw new UnsupportedOperationException()

    override def isEmpty: Boolean = list.isEmpty

    override def containsAll(c: util.Collection[_]): Boolean = list.containsSlice(c.toArray)

    override def addAll(c: util.Collection[_ <: A]): Boolean = {
        c.forEach(add)
        !c.isEmpty
    }

    override def removeAll(c: util.Collection[_]): Boolean = {
        c.forEach(e => remove(e))
        !c.isEmpty
    }

    override def retainAll(c: util.Collection[_]): Boolean = {
        list.filterInPlace(c.contains)
        true
    }

    override def clear(): Unit = list.clear()

    override def toArray: Array[AnyRef] = {
        val buff = ListBuffer.empty[Any]
        list.foreach(e => buff.addOne(e))
        buff.toArray.asInstanceOf[Array[AnyRef]]
    }

    override def toString: String = list.toArray[Any].mkString("ProvidedBlockingQueue(", ", ", ")")

    override def toArray[T](a: Array[T with Object]): Array[T with Object] = {
        toArray.asInstanceOf[Array[T with Object]]
    }


}
