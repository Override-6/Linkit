package fr.`override`.linkkit.server.config

import fr.`override`.linkkit.api.system.config.RelayConfiguration
import fr.`override`.linkkit.api.system.security.RelaySecurityManager
import fr.`override`.linkkit.server.RelayServer
import fr.`override`.linkkit.server.security.RelayServerSecurityManager

trait RelayServerConfiguration extends RelayConfiguration {
    val port: Int

    val maxConnection: Int

    val relayIDAmbiguityStrategy: AmbiguityStrategy
    override val securityManager: RelayServerSecurityManager

    override val identifier: String = RelayServer.Identifier

}