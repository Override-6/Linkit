package fr.`override`.linkit.api.local.system.config

import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.local.system.security.BytesHasher

import java.net.InetSocketAddress

trait ConnectionConfiguration {

    val identifier: String
    val remotePort: Int
    val remoteAddress: InetSocketAddress

    val securityManager: BytesHasher
    val translator: PacketTranslator

}
