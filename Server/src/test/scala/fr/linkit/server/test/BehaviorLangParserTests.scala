package fr.linkit.server.test

import fr.linkit.engine.internal.language.bhv.ContractImpl
import fr.linkit.lib.behavior.ObjectsProperty
import fr.linkit.server.test.ServerLauncher.Port
import org.junit.jupiter.api.Test

class BehaviorLangParserTests {

    private val app     = ServerLauncher.launch()
    private val network = app.findConnection(Port).get.network
}