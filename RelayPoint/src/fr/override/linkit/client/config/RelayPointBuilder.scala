package fr.`override`.linkit.client.config

import java.net.InetSocketAddress

import fr.`override`.linkit.client.{RelayPoint, RelayPointSecurityManager}
import fr.`override`.linkit.api.system.security.RelaySecurityManager
import fr.`override`.linkit.client.RelayPoint

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

      var serverAddress: InetSocketAddress
      var identifier: String

      def build(): RelayPoint = {
            val builder = this
            val configuration = new RelayPointConfiguration {
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
            }
            new RelayPoint(configuration)
      }

}

object RelayPointBuilder {

      implicit def autoBuild(builder: RelayPointBuilder): RelayPoint = {
            builder.build()
      }

}
