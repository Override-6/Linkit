package fr.overridescala.linkkit.client.config

import java.net.InetSocketAddress

import fr.overridescala.linkkit.client.RelayPoint

abstract class RelayPointBuilder {


    var enableExtensions: Boolean = true
    var enableTasks: Boolean = true
    var enableEventHandling: Boolean = false //as long as this stand an obsolete feature
    var enableRemoteConsoles: Boolean = true
    var checkReceivedPacketTargetID: Boolean = false

    var taskQueueSize: Int = 256
    var maxPacketLength: Int = Int.MaxValue - 8
    var defaultContainerPacketCacheSize: Int = 1024
    var maxPacketContainerCacheSize: Int = 8192

    var serverAddress: InetSocketAddress
    var identifier: String

    def build(): RelayPoint = {
        val builder = this
        val configuration = new RelayPointConfiguration {
            override val serverAddress: InetSocketAddress = builder.serverAddress

            override val enableExtensions: Boolean = builder.enableExtensions
            override val enableTasks: Boolean = builder.enableTasks
            override val enableEventHandling: Boolean = builder.enableEventHandling
            override val enableRemoteConsoles: Boolean = builder.enableRemoteConsoles
            override val checkReceivedPacketTargetID: Boolean = builder.checkReceivedPacketTargetID

            override val taskQueueSize: Int = builder.taskQueueSize
            override val maxPacketLength: Int = builder.maxPacketLength
            override val defaultContainerPacketCacheSize: Int = builder.defaultContainerPacketCacheSize
            override val maxPacketContainerCacheSize: Int = builder.maxPacketContainerCacheSize

            override val identifier: String = builder.identifier
            override val reconnectionPeriod: Int = 5000
            override val extensionsFolder: String = "/RelayExtensions/"
        }
        new RelayPoint(configuration)
    }

}

object RelayPointBuilder {

    implicit def autoBuild(builder: RelayPointBuilder): RelayPoint = {
        builder.build()
    }

}
