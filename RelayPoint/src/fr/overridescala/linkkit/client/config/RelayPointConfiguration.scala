package fr.overridescala.linkkit.client.config

import java.net.InetSocketAddress

import fr.overridescala.linkkit.api.system.config.RelayConfiguration

trait RelayPointConfiguration extends RelayConfiguration {
    val serverAddress: InetSocketAddress

    val reconnectionPeriod: Int //time to reconnect in ms
}
