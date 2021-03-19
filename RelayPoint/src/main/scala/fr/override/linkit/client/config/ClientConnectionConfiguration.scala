package fr.`override`.linkit.client.config

import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration

trait ClientConnectionConfiguration extends ConnectionConfiguration {
    val reconnectionPeriod: Int //time to reconnect in ms
}
