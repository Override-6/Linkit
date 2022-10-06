package fr.linkit.engine.internal.debug

trait WorkersDump {

    def getPool(name: String): WorkerPoolDump

    def getAll: Iterator[WorkerPoolDump]

}