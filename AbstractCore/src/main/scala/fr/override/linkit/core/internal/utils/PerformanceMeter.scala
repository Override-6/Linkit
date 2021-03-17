package fr.`override`.linkit.core.internal.utils

class PerformanceMeter {

    private var reference = now

    def printPerf(): Unit = {
        val t = now
        println(s"time passed between now and last reference : ${t - reference}")
        reference = now
    }

    def printPerf(cause: String): Unit = {
        val t = now
        println(s"time passed between now and last reference : ${t - reference} ($cause)")
        reference = now
    }

    private def now: Long = System.currentTimeMillis()

}
