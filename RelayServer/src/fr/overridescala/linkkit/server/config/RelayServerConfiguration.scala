package fr.overridescala.linkkit.server.config

import fr.overridescala.linkkit.api.system.config.RelayConfiguration
import fr.overridescala.linkkit.api.system.security.RelaySecurityManager
import fr.overridescala.linkkit.server.RelayServer
import fr.overridescala.linkkit.server.security.RelayServerSecurityManager

trait RelayServerConfiguration extends RelayConfiguration {
    val port: Int

    val maxConnection: Int

    val relayIDAmbiguityStrategy: AmbiguityStrategy
    override val securityManager: RelayServerSecurityManager

    override val identifier: String = RelayServer.Identifier

}