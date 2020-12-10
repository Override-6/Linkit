package fr.overridescala.linkkit.server

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.server.config.{AmbiguityStrategy, RelayServerBuilder}

object Main {
    def main(args: Array[String]): Unit = {
        val relayServer: Relay = new RelayServerBuilder {
            relayIDAmbiguityStrategy = AmbiguityStrategy.REJECT_NEW
        }
        relayServer.start()
    }
}
