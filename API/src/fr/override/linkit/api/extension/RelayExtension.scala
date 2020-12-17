package fr.`override`.linkit.api.`extension`

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.Relay

abstract class RelayExtension(protected val relay: Relay) {

    implicit protected val self: RelayExtension = this

    def onEnable(): Unit

    def onDisable(): Unit = ()
}