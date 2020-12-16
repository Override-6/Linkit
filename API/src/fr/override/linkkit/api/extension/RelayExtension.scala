package fr.`override`.linkkit.api.`extension`

import fr.`override`.linkkit.api.Relay
import fr.`override`.linkkit.api.Relay

abstract class RelayExtension(protected val relay: Relay) {

    implicit protected val self: RelayExtension = this

    def onEnable(): Unit

    def onDisable(): Unit = ()
}