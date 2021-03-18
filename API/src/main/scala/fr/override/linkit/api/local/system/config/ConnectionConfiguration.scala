package fr.`override`.linkit.api.local.system.config

import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.local.system.security.ConnectionSecurityManager

trait ConnectionConfiguration {

    val identifier: String

    val securityManager: ConnectionSecurityManager
    val translator: PacketTranslator

}
