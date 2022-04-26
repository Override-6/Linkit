package fr.linkit.server.test

import fr.linkit.engine.internal.manipulation.invokation.MethodInvoker
import org.junit.jupiter.api.Test

class NativeTests {
    ServerLauncher.launch()

    def method(s: String, n: NativeTests, f: Float, i: Int, b: Boolean, j: Long): Unit = {
        print(s, n, f, i, b, j)
    }

    @Test
    def methodCaller(): Unit = {
        val m = getClass.getDeclaredMethods.find(_.getName == "method").get
        val inv = new MethodInvoker(m)
        val f = Array("hey", this, 0.8f, 7, true, 8.toLong)
        inv.invoke(this, f)
    }


}
