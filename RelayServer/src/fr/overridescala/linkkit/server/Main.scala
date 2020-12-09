package fr.overridescala.linkkit.server

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.server.config.RelayServerBuilder

object Main {
    def main(args: Array[String]): Unit = {
        val relayServer: Relay = new RelayServerBuilder().build()
        relayServer.start()
    }
}
