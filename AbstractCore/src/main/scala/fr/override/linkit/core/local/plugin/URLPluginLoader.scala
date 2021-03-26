package fr.`override`.linkit.core.local.plugin

import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.plugin.{Plugin, PluginLoadException, PluginLoader}

import scala.util.control.NonFatal

class URLPluginLoader(context: ApplicationContext, loader: PluginClassLoader) extends PluginLoader{

    @volatile private var count = 0

    override def nextPlugin(): Plugin = {
        val adapter = loader.pluginFiles(count)
        val mainClass = loader.loadMainClass(adapter)
        count += 1
        try {
            val plugin = mainClass.getConstructor().newInstance()
            plugin.init(context)
            plugin
        } catch {
            case _: NoSuchMethodException =>
                throw PluginLoadException(s"Could not load '${mainClass.getName}' : empty constructor is missing !")
            case NonFatal(e) =>
                throw PluginLoadException(s"Could not load '${mainClass.getName}' : ${e.getMessage}", e)
        }
    }

    override def currentIndex: Int = count

    override def length: Int = loader.countUrls

    override def haveNextPlugin: Boolean = count < length
}
