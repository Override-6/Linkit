package fr.linkit.server.test

import fr.linkit.engine.internal.manipulation.invokation.MethodInvoker
import org.junit.jupiter.api.Test

import java.nio.file.Path

class NativeTests {
    ServerLauncher.launch()

    def method(s: String, n: NativeTests, f: Float, i: Int, b: Boolean, j: Long): Unit = {
        print(s, n, f, i, b, j)
    }

    def method2(): Unit = {

    }

    @Test
    def methodCaller(): Unit = {
        val target = Path.of("C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home")
        val m = target.getClass.getMethods.find(_.getName == "hashCode").get
        val inv = new MethodInvoker(m)
        val f = Array[Any]()
        val x = inv.invoke(target, f)
        print(s"yeay: ${x}")
    }
}