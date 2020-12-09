package fr.overridescala.linkkit.api.system.config

trait RelayConfiguration {

    val enableExtensions: Boolean
    val enableTasks: Boolean
    val enableEventHandling: Boolean
    val enableRemoteConsoles: Boolean
    val checkReceivedPacketTargetID: Boolean

    val taskQueueSize: Int
    val maxPacketLength: Int //only concern custom bytes length
    val defaultContainerPacketCacheSize: Int //numbers of packet a PacketContainer can contain
    val maxPacketContainerCacheSize: Int //max registered PacketContainer in a TrafficHandler

    val extensionsFolder: String //can be relative or global
    val identifier: String

}
