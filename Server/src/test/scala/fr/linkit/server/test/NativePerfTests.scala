package fr.linkit.server.test

import fr.linkit.engine.internal.manipulation.invokation.MethodInvoker
import org.junit.jupiter.api.Test

class NativePerfTests {
    ServerLauncher.launch()

    def method(s: String, n: NativePerfTests, f: Float, i: Int, b: Boolean, j: Long): Unit = {
        print(s, n, f, i, b, j)
    }

    @Test
    def methodCaller(): Unit = {
        val m = getClass.getDeclaredMethods().find(_.getName == "method").get
        val inv = new MethodInvoker(m)
        for (i <- 0 to 10000) {
            val s0 = System.nanoTime()
            inv.invoke(this, Array("hey", this, 0.8f, i, true, i.toLong))
            val s1 = System.nanoTime()
            println(s" $i - took ${s1 - s0} ns.")
        }
        for (i <- 0 to 10000) {
            val s0 = System.nanoTime()
            m.invoke(this, "hey", this, 0.8f, i, true, i.toLong)
            val s1 = System.nanoTime()
            println(s"$i - took ${s1 - s0} ns.")
        }
    }


}
