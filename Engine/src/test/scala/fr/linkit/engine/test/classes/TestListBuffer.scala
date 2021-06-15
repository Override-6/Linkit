package fr.linkit.engine.test.classes

import scala.collection.mutable.ListBuffer

class TestListBuffer[A] extends ListBuffer[A] {
    override def tapEach[U](f: A => U): ListBuffer[A] = super.tapEach(f)

    override def compose[S](g: S => Int): S => A = super.compose[S](g)

}
