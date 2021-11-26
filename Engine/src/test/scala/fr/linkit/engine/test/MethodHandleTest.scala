package fr.linkit.engine.test

import java.lang.invoke.{MethodHandles, MethodType}

import org.junit.jupiter.api.Test


class MethodHandleTest {

    @Test
    def startTest(): Unit = {
        val player       = Player(89, "hey", "ho", 78, 789)
        val lookup       = MethodHandles.lookup()
        val ct           = MethodType.methodType(Void.TYPE, Array[Class[_]](classOf[Int], classOf[String], classOf[String], classOf[Int], classOf[Int]))
        val methodHandle = lookup.findConstructor(player.getClass, ct)
        println(s"methodHandle = ${methodHandle}")
        val test = methodHandle.invoke(84, "WOW", "WORKED", 8, 0)
        println(s"player = ${player}")
    }

}
