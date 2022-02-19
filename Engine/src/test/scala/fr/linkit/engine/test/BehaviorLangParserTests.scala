package fr.linkit.engine.test

import org.junit.jupiter.api.Test

class BehaviorLangParserTests {

    @Test
    def parse(): Unit = {
        val behaviorSource = new String(getClass.getResourceAsStream("/FSControl.bhv").readAllBytes())
        val imports = ImportParser.retrieveImports(behaviorSource)
        println(s"imports = ${imports}")
    }

}
