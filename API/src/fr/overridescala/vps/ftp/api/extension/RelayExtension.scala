package fr.overridescala.vps.ftp.api.`extension`

import fr.overridescala.vps.ftp.api.Relay

abstract class RelayExtension(protected val relay: Relay) {

    implicit protected val self: RelayExtension = this

    def main(): Unit

}