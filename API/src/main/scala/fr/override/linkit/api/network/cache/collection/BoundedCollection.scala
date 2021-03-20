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

package fr.`override`.linkit.api.network.cache.collection

import fr.`override`.linkit.api.network.cache.collection.BoundedCollection.{Immutable, Mutator}
import fr.`override`.linkit.api.network.cache.collection.CollectionModification._
import fr.`override`.linkit.api.utils.ConsumerContainer

import java.util.ConcurrentModificationException
import scala.collection.mutable.ListBuffer

class BoundedCollection[A, B](map: A => B) extends Mutator[A] with Immutable[B] {

    private val collection: ListBuffer[B] = ListBuffer.empty
    private val listeners = ConsumerContainer[(CollectionModification, Int, Option[B])]()
    @volatile private var specificModCount = 0;

    override def iterator: Iterator[B] = collection.iterator

    override def set(array: Array[A]): Unit = {
        specificModCount += 1
        collection.clear()
        array.foreach(add)
    }

    override def add(e: A): Unit = {

        val el = safeMap(e)
        listeners.applyAll((ADD, size - 1, Option(el)))
        collection += el
    }

    override def add(a: Int, e: A): Unit = {

        val el = safeMap(e)
        listeners.applyAll((ADD, a, Option(el)))
        collection.insert(a, el)
    }

    override def remove(i: Int): Unit = {
        specificModCount += 1


        val el = collection.remove(i)
        listeners.applyAll((REMOVE, i, Option(el)))
    }

    override def clear(): Unit = {
        specificModCount += 1

        listeners.applyAll((CLEAR, -1, None))
        collection.clear()
    }

    override def set(i: Int, a: A): Unit = {
        val el = safeMap(a)

        listeners.applyAll((SET, i, Option(el)))
        collection.update(i, el)
    }

    override def addListener(callback: (CollectionModification, Int, Option[B]) => Unit): this.type= {
        listeners += (tuple3 => callback(tuple3._1, tuple3._2, tuple3._3))
        this
    }

    private def safeMap(a: A): B = {
        val lastModCount = specificModCount
        val t: Any = map(a)
        val v = t.asInstanceOf[B]
        if (specificModCount != lastModCount)
            throw new ConcurrentModificationException("Bounded Collection got modified during mapping.")
        v
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

        def addListener(callback: (CollectionModification, Int, Option[A]) => Unit): this.type
    }

}
