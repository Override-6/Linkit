package fr.linkit.engine.test

import fr.linkit.engine.internal.language.bhv.Contract
import org.junit.jupiter.api.Test

class BehaviorLangParserTests {

    @Test
    def parse(): Unit = {
        Contract("/FSControl.bhv")
    }

}
