package fr.overridescala.linkkit.api.`extension`

import fr.overridescala.linkkit.api.Relay

abstract class RelayExtension(protected val relay: Relay) {

    implicit protected val self: RelayExtension = this

    def main(): Unit

}