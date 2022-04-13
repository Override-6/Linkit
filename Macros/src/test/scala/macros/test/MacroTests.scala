package macros.test

import fr.linkit.macros.ClassStaticAccessorGenerator.property

class MacroTests {

    class Test() {
        @property("test")
        val x: String = ""
    }

    @Test
    def test(): Unit = {
        val x = "ClassStaticAccessorGenerator.newStaticAccessor[Path]"
        println("x")
    }

}
