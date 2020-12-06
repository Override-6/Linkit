package fr.overridescala.linkkit.server

import fr.overridescala.linkkit.api.Relay

object Main {
    def main(args: Array[String]): Unit = {
        val relayServer: Relay = new RelayServer()
        relayServer.start()
    }
}
