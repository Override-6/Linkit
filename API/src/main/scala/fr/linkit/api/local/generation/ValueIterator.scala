package fr.linkit.api.local.generation

trait ValueIterator[A, B] {

    def foreach(a: A, action: B => Unit): Unit

}
