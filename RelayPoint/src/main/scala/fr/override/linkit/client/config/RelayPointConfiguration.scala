package fr.`override`.linkit.client.config

import java.net.InetSocketAddress

import fr.`override`.linkit.api.system.config.RelayConfiguration

trait RelayPointConfiguration extends RelayConfiguration {
    val serverAddress: InetSocketAddress

    val reconnectionPeriod: Int //time to reconnect in ms
}
