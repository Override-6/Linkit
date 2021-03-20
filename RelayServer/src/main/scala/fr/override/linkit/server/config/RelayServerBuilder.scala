package fr.`override`.linkit.server.config

import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.security.ServerSecurityManager

class RelayServerBuilder {

    var enableExtensionsFolderLoad: Boolean = true
    var enableTasks: Boolean = true
    var enableEventHandling: Boolean = false //As long as this stand an obsolete feature
    var enableRemoteConsoles: Boolean = true
    var checkReceivedPacketTargetID: Boolean = false //No effect for Server as it will automatically deflect packet if the targetID of packet coordinates isn't the server identifier

    var taskQueueLength: Int = 256
    var maxPacketLength: Int = Int.MaxValue - 8
    var maxPacketContainerCacheLength: Int = 1024
    var packetCacheLength: Int = 8192

    var securityManager: ServerSecurityManager = ServerSecurityManager.default()
    var fsAdapter: FileSystemAdapter = JDKFileSystemAdapters.Nio

    var port: Int = 48484
    var maxConnection: Int = Int.MaxValue
    var relayIDAmbiguityStrategy: AmbiguityStrategy = AmbiguityStrategy.REJECT_NEW
    var extensionsFolder: String = "/RelayExtensions/"

    def build(): RelayServer = {
        val builder = this
        val configuration = new ServerConnectionConfiguration {
            override val enableExtensionsFolderLoad: Boolean = builder.enableExtensionsFolderLoad
            override val enableTasks: Boolean = builder.enableTasks
            override val enableEventHandling: Boolean = builder.enableEventHandling
            override val enableRemoteConsoles: Boolean = builder.enableRemoteConsoles
            override val checkReceivedPacketTargetID: Boolean = builder.checkReceivedPacketTargetID

            override val taskQueueSize: Int = builder.taskQueueLength
            override val maxPacketLength: Int = builder.maxPacketLength
            override val defaultContainerPacketCacheSize: Int = builder.maxPacketContainerCacheLength
            override val maxPacketContainerCacheSize: Int = builder.maxPacketContainerCacheLength

            override val securityManager: ServerSecurityManager = builder.securityManager
            override val fsAdapter: FileSystemAdapter = builder.fsAdapter

            override val port: Int = builder.port
            override val maxConnection: Int = builder.maxConnection
            override val relayIDAmbiguityStrategy: AmbiguityStrategy = builder.relayIDAmbiguityStrategy
            override val extensionsFolder: String = builder.extensionsFolder
        }: ServerConnectionConfiguration
        new RelayServer(configuration)
    }

}

//TODO Java style Builder
object RelayServerBuilder {

    implicit def autoBuild(builder: RelayServerBuilder): RelayServer = {
        builder.build()
    }

}
