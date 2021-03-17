package fr.`override`.linkit.skull.internal

import fr.`override`.linkit.skull.connection.ConnectionContext
import fr.`override`.linkit.skull.internal.system.config.ConnectionConfiguration

trait LinkitApplicationContext {

    def bind(config: ConnectionConfiguration): ConnectionContext



}
