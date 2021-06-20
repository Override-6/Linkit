package fr.linkit.engine.test.classes

import scala.collection.mutable.ListBuffer

class TestListBuffer[A](other: TestListBuffer[A]) extends ListBuffer[A] {
    override def tapEach[U](f: A => U): ListBuffer[A] = super.tapEach(f)

    class AC {
        def test(t: this.type): this.type = ???
    }

    class B extends AC {

//        override def test(t: B): B = ???
    }
}
