package fr.overridescala.linkkit.api.system.config

import fr.overridescala.linkkit.api.system.security.RelaySecurityManager

trait RelayConfiguration {

    val enableExtensionsFolderLoad: Boolean
    val enableTasks: Boolean
    val enableEventHandling: Boolean
    val enableRemoteConsoles: Boolean
    val checkReceivedPacketTargetID: Boolean

    val taskQueueSize: Int
    val maxPacketLength: Int //only concern custom bytes length
    val defaultContainerPacketCacheSize: Int //numbers of packet a PacketContainer can contain
    val maxPacketContainerCacheSize: Int //max registered PacketContainer in a TrafficHandler

    val securityManager: RelaySecurityManager

    val extensionsFolder: String //can be relative or global
    val identifier: String

}
