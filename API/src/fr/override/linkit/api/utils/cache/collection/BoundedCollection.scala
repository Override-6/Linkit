package fr.`override`.linkit.api.utils.cache.collection

import fr.`override`.linkit.api.utils.ConsumerContainer
import fr.`override`.linkit.api.utils.cache.collection.BoundedCollection.{Immutable, Mutator}
import fr.`override`.linkit.api.utils.cache.collection.CollectionModification._

import scala.collection.mutable.ListBuffer

class BoundedCollection[A, B](map: A => B) extends Mutator[A] with Immutable[B] {
    private val collection: ListBuffer[B] = ListBuffer.empty
    private val listeners = ConsumerContainer[(CollectionModification, Int, Option[B])]()

    override def iterator: Iterator[B] = collection.iterator

    override def set(array: Array[A]): Unit = {
        collection.clear()
        array.foreach(add)
    }

    override def add(e: A): Unit = {
        val el = map(e)
        collection += el
        listeners.applyAllAsync((ADD, size - 1, Option(el)))
    }

    override def add(a: Int, e: A): Unit = {
        val el = map(e)
        collection.insert(a, el)
        listeners.applyAllAsync((ADD, a, Option(el)))
    }

    override def remove(i: Int): Unit = {
        val el = collection.remove(i)
        listeners.applyAllAsync((REMOVE, i, Option(el)))
    }

    override def clear(): Unit = {
        collection.clear()
        listeners.applyAllAsync((CLEAR, -1, None))
    }

    override def set(i: Int, a: A): Unit = {
        val el = map(a)
        collection.update(i, el)
        listeners.applyAllAsync((SET, i, Option(el)))
    }

    override def addListener(callback: (CollectionModification, Int, Option[B]) => Unit): Unit = {
        listeners += (tuple3 => callback(tuple3._1, tuple3._2, tuple3._3))
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
