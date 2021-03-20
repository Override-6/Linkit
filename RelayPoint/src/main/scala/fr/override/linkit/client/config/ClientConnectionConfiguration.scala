package fr.`override`.linkit.client.config

import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration

import java.net.{InetSocketAddress, Socket}

trait ClientConnectionConfiguration extends ConnectionConfiguration {
    val reconnectionPeriod: Int //time to reconnect in ms
    val socketFactory: InetSocketAddress => Socket
}
