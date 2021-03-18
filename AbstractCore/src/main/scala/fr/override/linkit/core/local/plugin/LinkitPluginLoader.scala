package fr.`override`.linkit.core.local.plugin

import fr.`override`.linkit.api.local.plugin.{LinkitPlugin, Plugin, PluginLoadException, PluginLoader}

import scala.util.control.NonFatal

class LinkitPluginLoader(classes: Seq[Class[_ <: LinkitPlugin]]) extends PluginLoader {
    private var count = 0

    override def nextPlugin(): Plugin = {
        if (!haveNextPlugin)
            throw new NoSuchElementException
        val clazz = classes(count)
        try {
            val plugin = clazz.getConstructor().newInstance()
            plugin.init(this)
            plugin
        } catch {
            case _: NoSuchMethodException =>
                throw PluginLoadException(s"Could not load '${clazz.getSimpleName}' : constructor() is missing !")
            case NonFatal(e) =>
                throw PluginLoadException(s"Could not load '${clazz.getSimpleName}' : ${e.getMessage}", e)
        } finally {
            count += 1
        }
    }

    override def currentIndex(): Int = count

    override def length: Int = classes.length

    override def haveNextPlugin: Boolean = count >= classes.length


}