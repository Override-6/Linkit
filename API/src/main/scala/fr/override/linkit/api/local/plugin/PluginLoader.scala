package fr.`override`.linkit.api.local.plugin

import java.io.Closeable

trait PluginLoader extends Closeable {

    @throws[PluginLoadException]
    def nextPlugin(): Plugin

    def currentIndex(): Int

    def length: Int

    def haveNextPlugin: Boolean

}
