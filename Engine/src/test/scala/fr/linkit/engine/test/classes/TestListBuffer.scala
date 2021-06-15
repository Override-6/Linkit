package fr.linkit.engine.test.classes

import scala.collection.mutable.ListBuffer

class TestListBuffer[A] extends ListBuffer[A] {
    override def tapEach[U](f: A => U): ListBuffer[A] = super.tapEach(f)

    super.tapEach(null)
}
