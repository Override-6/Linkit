package fr.`override`.linkit.api.local.plugin

import fr.`override`.linkit.api.local.plugin.fragment.PluginFragment

trait Plugin {
    def getLoader: PluginLoader

    def onLoad(): Unit

    def onEnable(): Unit

    def onDisable(): Unit
}