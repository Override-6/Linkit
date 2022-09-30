package fr.linkit.test.natives

import fr.linkit.engine.internal.manipulation.invokation.MethodInvoker
import fr.linkit.test.TestEngine
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

@TestInstance(Lifecycle.PER_CLASS)
class NativeTests {
    TestEngine.launchMockingNetwork() //load the class

    private val method = getClass.getDeclaredMethods.find(_.getName == "foo").get
    private val invoker = new MethodInvoker(method)

    private def foo(a: Int, b: Byte, c: Boolean, d: Float,
                    e: Double, f: Long, g: Char, h: Short, i: Object): (Int, Byte, Boolean, Float, Double, Long, Char, Short, Object) = {
        (a, b, c, d, e, f, g, h, i)
    }

    @Test
    def testPrimitivesConversion(): Unit = {
        implicit def cast[X](a: Any): X = a.asInstanceOf[X]

        val obj = ("I'm an object !", 4, 7.5)
        val (a, b, c, d, e, f, g, h, i) = invoker.invoke(this, Array(Int.MinValue, Byte.MinValue, false, Float.MinValue, Double.MinValue, Long.MinValue, 'A', Short.MinValue, obj))
        Assertions.assertEquals(a, Int.MinValue)
        Assertions.assertEquals(b, Byte.MinValue)
        Assertions.assertEquals(c, false)
        Assertions.assertEquals(d, Float.MinValue)
        Assertions.assertEquals(e, Double.MinValue)
        Assertions.assertEquals(f, Long.MinValue)
        Assertions.assertEquals(g, 'A')
        Assertions.assertEquals(h, Short.MinValue)
        Assertions.assertSame(i, obj)
    }


    private final val Reps = 1000

    //@Test
    def performanceTest(): Unit = {
        var i    = 0
        val args = Array(Int.MinValue, Byte.MinValue, false, Float.MinValue, Double.MinValue, Long.MinValue, 'A', Short.MinValue, ("I'm an object !", 4, 7.5))
        invoker.invoke(this, args) //ensure a first call to warm up vm

        val t0 = System.currentTimeMillis()
        while (i <= Reps) {
            invoker.invoke(this, args)
            i += 1
        }
        val t1 = System.currentTimeMillis()
        println(s"(native invoker) took ${t1-t0}ms. (average of ${(t1-t0)/Reps} ms per call)")
        method.setAccessible(true)
        val t2 = System.currentTimeMillis()
        while (i <= Reps) {
            method.invoke(this, args)
            i += 1
        }
        val t3 = System.currentTimeMillis()
        println(s"(via reflection lib) took ${t3-t2}ms. (average of ${(t3-t2)/Reps} ms per call)")

    }


}
