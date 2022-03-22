package fr.linkit.server.test

import fr.linkit.engine.internal.language.bhv.{Contract, ObjectsProperty}
import fr.linkit.server.test.ServerLauncher.Port
import org.junit.jupiter.api.Test

class BehaviorLangParserTests {

    private val app     = ServerLauncher.launch()
    private val network = app.findConnection(Port).get.network

    @Test
    def parse(): Unit = {
        Contract("contracts/NetworkContract.bhv")(app, ObjectsProperty.default(network))
    }

}
