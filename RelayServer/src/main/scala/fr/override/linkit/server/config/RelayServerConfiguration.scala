package fr.`override`.linkit.server.config

import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.security.RelayServerSecurityManager
import fr.`override`.linkit.api.system.config.RelayConfiguration
import fr.`override`.linkit.api.system.security.RelaySecurityManager

trait RelayServerConfiguration extends RelayConfiguration {
    val port: Int

    val maxConnection: Int

    val relayIDAmbiguityStrategy: AmbiguityStrategy
    override val securityManager: RelayServerSecurityManager

    override val identifier: String = RelayServer.Identifier

}