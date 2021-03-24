package fr.`override`.linkit.api.local.plugin

import java.security.Permission

import fr.`override`.linkit.api.local.system.Version

trait PluginConfiguration {
    val pluginName: String

    val pluginDescription: String

    val pluginVersion: Version

    val pluginPermissions: Array[Permission]

    def getProperty(name: String): String
}
