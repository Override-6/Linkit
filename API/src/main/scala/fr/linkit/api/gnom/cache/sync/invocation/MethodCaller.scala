package fr.linkit.api.gnom.cache.sync.invocation

trait MethodCaller {

    def call(name: String, args: Array[Any]): Any

}
