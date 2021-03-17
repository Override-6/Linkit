package fr.`override`.linkit.client.config

import java.net.InetSocketAddress

import fr.`override`.linkit.skull.internal.system.fsa.FileSystemAdapter
import fr.`override`.linkit.skull.internal.system.security.RelaySecurityManager
import fr.`override`.linkit.client.{RelayPoint, RelayPointSecurityManager}

abstract class RelayPointBuilder {

    var enableExtensionsFolderLoad: Boolean = true
    var enableTasks: Boolean = true
    var enableEventHandling: Boolean = false //as long as this stand an obsolete feature
    var enableRemoteConsoles: Boolean = true
    var checkReceivedPacketTargetID: Boolean = false

    var taskQueueSize: Int = 256
    var maxPacketLength: Int = Int.MaxValue - 8
    var defaultContainerPacketCacheSize: Int = 1024
    var maxPacketContainerCacheSize: Int = 8192
    var reconnectionPeriod: Int = 5000
    var extensionsFolder: String = "/RelayExtensions/"

    var securityManager: RelaySecurityManager = new RelayPointSecurityManager
    var fsAdapter: FileSystemAdapter = JDKFileSystemAdapters.Nio

    var serverAddress: InetSocketAddress
    var identifier: String

    def build(): RelayPoint = {
        val configuration = genConfig
        new RelayPoint(configuration)
    }

    /**
     * @return a RelayPointConfiguration based the current settings
     * */
    def genConfig: ClientConnectionConfiguration = {
        val builder = this
        new ClientConnectionConfiguration {
            override val serverAddress: InetSocketAddress = builder.serverAddress

            override val enableExtensionsFolderLoad: Boolean = builder.enableExtensionsFolderLoad
            override val enableTasks: Boolean = builder.enableTasks
            override val enableEventHandling: Boolean = builder.enableEventHandling
            override val enableRemoteConsoles: Boolean = builder.enableRemoteConsoles
            override val checkReceivedPacketTargetID: Boolean = builder.checkReceivedPacketTargetID

            override val taskQueueSize: Int = builder.taskQueueSize
            override val maxPacketLength: Int = builder.maxPacketLength
            override val defaultContainerPacketCacheSize: Int = builder.defaultContainerPacketCacheSize
            override val maxPacketContainerCacheSize: Int = builder.maxPacketContainerCacheSize

            override val identifier: String = builder.identifier
            override val reconnectionPeriod: Int = builder.reconnectionPeriod
            override val extensionsFolder: String = builder.extensionsFolder

            override val securityManager: RelaySecurityManager = builder.securityManager
            override val fsAdapter: FileSystemAdapter = builder.fsAdapter
        }
    }

}

object RelayPointBuilder {

    implicit def autoBuild(builder: RelayPointBuilder): RelayPoint = {
        builder.build()
    }

    def javaBuilder(serverAddress: InetSocketAddress, identifier: String): JavaBuilder = {
        new JavaBuilder(serverAddress, identifier)
    }

    class JavaBuilder(serverAddress: InetSocketAddress, identifier: String) {
        private val scalaBuilder: RelayPointBuilder = {
            val serv = serverAddress
            val id = identifier
            new RelayPointBuilder {
                override var serverAddress: InetSocketAddress = serv
                override var identifier: String = id
            }
        }

        def enableExtensionsFolderLoad(enabled: Boolean): this.type = {
            scalaBuilder.enableExtensionsFolderLoad = enabled
            this
        }

        def enableTasks(enabled: Boolean): this.type = {
            scalaBuilder.enableTasks = enabled
            this
        }

        def enableEventHandling(enabled: Boolean): this.type = {
            scalaBuilder.enableEventHandling = enabled
            this
        }

        def enableRemoteConsoles(enabled: Boolean): this.type = {
            scalaBuilder.enableRemoteConsoles = enabled
            this
        }

        def checkReceivedPacketTargetID(enabled: Boolean): this.type = {
            scalaBuilder.checkReceivedPacketTargetID = enabled
            this
        }

        def setTaskQueueSize(value: Int): this.type = {
            scalaBuilder.taskQueueSize = value
            this
        }

        def setMaxPacketLength(value: Int): this.type = {
            scalaBuilder.maxPacketLength = value
            this
        }

        def setDefaultContainerPacketCacheSize(value: Int): this.type = {
            scalaBuilder.defaultContainerPacketCacheSize = value
            this
        }

        def setMaxPacketContainerCacheSize(value: Int): this.type = {
            scalaBuilder.maxPacketContainerCacheSize = value
            this
        }

        def setReconnectionPeriod(value: Int): this.type = {
            scalaBuilder.reconnectionPeriod = value
            this
        }

        def withExtensionFolder(path: String): this.type = {
            scalaBuilder.extensionsFolder = path
            this
        }

        def withSecurityManager(securityManager: RelayPointSecurityManager): this.type = {
            scalaBuilder.securityManager = securityManager
            this
        }

        def withFsAdapter(fsa: FileSystemAdapter): this.type = {
            scalaBuilder.fsAdapter = fsa
            this
        }

        def build: RelayPoint = {
            val configuration = scalaBuilder.genConfig
            new RelayPoint(configuration)
        }
    }

}
