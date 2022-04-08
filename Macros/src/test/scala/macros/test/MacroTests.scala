package macros.test

import fr.linkit.macros.ClassStaticAccessorGenerator
import org.junit.jupiter.api.Test

import java.nio.file.Path

class MacroTests {

    @Test
    def test(): Unit = {
        scala.language.reflectiveCalls
        val x = ClassStaticAccessorGenerator.newStaticAccessor[Path]
        x.of("XXXX")
    }

}
