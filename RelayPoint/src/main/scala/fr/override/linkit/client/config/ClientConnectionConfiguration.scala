package fr.`override`.linkit.client.config

import java.net.InetSocketAddress

import fr.`override`.linkit.skull.internal.system.config.ConnectionConfiguration

trait ClientConnectionConfiguration extends ConnectionConfiguration {
    val serverAddress: InetSocketAddress

    val reconnectionPeriod: Int //time to reconnect in ms
}
