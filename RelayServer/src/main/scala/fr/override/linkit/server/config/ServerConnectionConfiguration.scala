package fr.`override`.linkit.server.config

import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.security.RelayServerSecurityManager
import fr.`override`.linkit.skull.internal.system.config.ConnectionConfiguration
import fr.`override`.linkit.skull.internal.system.security.RelaySecurityManager

trait ServerConnectionConfiguration extends ConnectionConfiguration {
    val port: Int

    val maxConnection: Int

    val relayIDAmbiguityStrategy: AmbiguityStrategy
    override val securityManager: RelayServerSecurityManager

    override val identifier: String = RelayServer.Identifier

}