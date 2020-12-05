package fr.overridescala.vps.ftp.server

import fr.overridescala.vps.ftp.api.Relay

object Main {
    def main(args: Array[String]): Unit = {
        val relayServer: Relay = new RelayServer()
        relayServer.start()
    }
}
