package fr.overridescala.vps.ftp.server.main

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.server.RelayServer

object Main {


    def main(args: Array[String]): Unit = {
        val relayServer: Relay = new RelayServer("server")
        relayServer.start()
    }


}
