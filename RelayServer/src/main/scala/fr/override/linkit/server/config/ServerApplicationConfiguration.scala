package fr.`override`.linkit.server.config

import fr.`override`.linkit.api.local.system.config.ApplicationConfiguration
import fr.`override`.linkit.api.local.system.security.BytesHasher

trait ServerApplicationConfiguration extends ApplicationConfiguration {

    val port: Int
    val maxConnection: Int

    val relayIDAmbiguityStrategy: AmbiguityStrategy
    val identifier: String
    val hasher: BytesHasher

}
