package fr.linkit.engine.test

import fr.linkit.engine.local.utils.ScalaUtils
import org.junit.jupiter.api.Test
//import org.openjdk.jol.vm.VM

class UnsafeTests {

    private val unsafe = ScalaUtils.findUnsafe()

    @Test
    def makeTest(): Unit = {
        /*val obj: AnyRef = "Sexe"
        val address = VM.current().addressOf(obj)
        unsafe.putObject(null, address, "zizi")
        println(s"address = ${address}")
        println(s"obj = ${obj}")*/
    }
}
