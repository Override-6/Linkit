package fr.`override`.linkit.client.config

import java.net.{InetSocketAddress, Socket}

import fr.`override`.linkit.api.local.system.config.ApplicationConfiguration

trait ClientApplicationConfiguration extends ApplicationConfiguration {
    val socketFactory: (Int, InetSocketAddress) => Socket
}
