package fr.`override`.linkit.api.network.cache.collection

import java.util.ConcurrentModificationException

import fr.`override`.linkit.api.network.cache.collection.BoundedCollection.{Immutable, Mutator}
import fr.`override`.linkit.api.network.cache.collection.CollectionModification._
import fr.`override`.linkit.api.utils.ConsumerContainer

import scala.collection.mutable.ListBuffer

class BoundedCollection[A, B](map: A => B) extends Mutator[A] with Immutable[B] {

    private val collection: ListBuffer[B] = ListBuffer.empty
    private val listeners = ConsumerContainer[(CollectionModification, Int, Option[B])]()
    @volatile private var modCount = 0;

    override def iterator: Iterator[B] = collection.iterator

    override def set(array: Array[A]): Unit = {
        modCount += 1

        collection.clear()
        array.foreach(add)
    }

    override def add(e: A): Unit = {
        modCount += 1

        val el = safeMap(e)
        listeners.applyAll((ADD, size - 1, Option(el)))
        collection += el
    }

    override def add(a: Int, e: A): Unit = {
        modCount += 1

        val el = safeMap(e)
        listeners.applyAll((ADD, a, Option(el)))
        collection.insert(a, el)
    }

    override def remove(i: Int): Unit = {
        modCount += 1

        val el = collection.remove(i)
        listeners.applyAll((REMOVE, i, Option(el)))
    }

    override def clear(): Unit = {
        modCount += 1

        listeners.applyAll((CLEAR, -1, None))
        collection.clear()
    }

    override def set(i: Int, a: A): Unit = {
        val el = safeMap(a)
        listeners.applyAll((SET, i, Option(el)))
        collection.update(i, el)
    }

    override def addListener(callback: (CollectionModification, Int, Option[B]) => Unit): Unit = {
        listeners += (tuple3 => callback(tuple3._1, tuple3._2, tuple3._3))
    }

    private def safeMap(a: A): B = {
        val lastModCount = modCount
        val b = map(a)
        if (modCount != lastModCount)
            throw new ConcurrentModificationException("Bounded Collection got modified durring mapping.")
        b
    }


}

object BoundedCollection {

    trait Mutator[A] {
        def set(array: Array[A]): Unit

        def add(e: A): Unit

        def add(a: Int, e: A): Unit

        def remove(i: Int): Unit

        def clear(): Unit

        def set(i: Int, a: A): Unit
    }

    trait Immutable[A] extends Iterable[A] {
        override def iterator: Iterator[A]

        def addListener(callback: (CollectionModification, Int, Option[A]) => Unit): Unit
    }

}
