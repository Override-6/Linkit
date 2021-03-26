package fr.`override`.linkit.client.config

import fr.`override`.linkit.api.local.system.config.schematic.{AppSchematic, EmptySchematic}
import fr.`override`.linkit.api.local.system.fsa.FileSystemAdapter
import fr.`override`.linkit.api.local.system.security.ApplicationSecurityManager
import fr.`override`.linkit.client.ClientApplication
import fr.`override`.linkit.core.local.system.fsa.JDKFileSystemAdapters

class ClientApplicationConfigBuilder {
    private final var enableEventHandling: Boolean = false //still in development

    var loadSchematic: AppSchematic[ClientApplication] = new EmptySchematic()
    var nWorkerThreadFunction: Int => Int = _ * 2 + 3 //2 threads per external connection + 3 thread for application.
    var pluginFolder: Option[String] = Some("/Plugins")
    var fsAdapter: FileSystemAdapter = JDKFileSystemAdapters.Nio
    var securityManager: ApplicationSecurityManager = ApplicationSecurityManager.none

    def buildConfig(): ClientApplicationConfiguration = {
        val builder = this
        new ClientApplicationConfiguration {
            override val loadSchematic: AppSchematic[ClientApplication] = builder.loadSchematic
            override val enableEventHandling: Boolean = builder.enableEventHandling
            override val nWorkerThreadFunction: Int => Int = builder.nWorkerThreadFunction
            override val pluginFolder: Option[String] = builder.pluginFolder
            override val fsAdapter: FileSystemAdapter = builder.fsAdapter
            override val securityManager: ApplicationSecurityManager = builder.securityManager
        }
    }
}

object ClientApplicationConfigBuilder {
    implicit def autoBuild(builder: ClientApplicationConfigBuilder): ClientApplicationConfiguration = {
        builder.buildConfig()
    }
}


