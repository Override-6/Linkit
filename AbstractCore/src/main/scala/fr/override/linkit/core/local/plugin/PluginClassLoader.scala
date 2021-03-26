package fr.`override`.linkit.core.local.plugin

import java.net.URLClassLoader
import java.util.Properties
import java.util.zip.ZipFile

import fr.`override`.linkit.api.local.plugin.{LinkitPlugin, Plugin, PluginLoadException}
import fr.`override`.linkit.api.local.system.AppException
import fr.`override`.linkit.api.local.system.fsa.FileAdapter
import fr.`override`.linkit.core.local.plugin.PluginClassLoader.{MainClassField, PropertiesFileName}

import scala.collection.mutable

class PluginClassLoader(private[plugin] val pluginFiles: Array[FileAdapter], bridge: PluginClassLoaderBridge)
    extends URLClassLoader(pluginFiles.map(_.toUri.toURL)) {

    private val map = mutable.HashMap.empty[FileAdapter, String]

    private[plugin] def setPluginName(file: FileAdapter, name: String): Unit = {
        ensureContained(file)
        map.put(file, name)
    }

    override def loadClass(name: String, resolve: Boolean): Class[_] = {
        try {
            super.loadClass(name, resolve)
        } catch {
            case e: ClassNotFoundException =>
                bridge.loadClass(name, this)
        }
    }

    def loadMainClass(file: FileAdapter): Class[_ <: Plugin] = {
        ensureContained(file)
        loadJar(file)
    }

    def countUrls: Int = pluginFiles.length

    private def loadJar(adapter: FileAdapter): Class[_ <: Plugin] = {

        val jarFile = new ZipFile(adapter.getPath)
        val propertyFile = jarFile.getEntry(PropertiesFileName)
        //Checking property presence
        if (propertyFile == null)
            throw PluginLoadException(s"Jar file $adapter must have a file called '$PropertiesFileName' in his root")

        //Checking property content
        val property = new Properties()
        property.load(jarFile.getInputStream(propertyFile))
        val className = property.getProperty(MainClassField)
        if (className == null)
            throw PluginLoadException(s"Jar file $adapter properties' must contains a field named '$MainClassField'")

        //Loading extension's main
        val clazz = loadClass(className)
        assertPluginClass(clazz)
    }

    private def assertPluginClass(clazz: Class[_]): Class[_ <: LinkitPlugin] = {
        if (!classOf[LinkitPlugin].isAssignableFrom(clazz)) {
            throw new AppException(s"Class '$clazz' must extends '${classOf[LinkitPlugin]}' to be loaded as a Relay extension.")
        }
        clazz.asInstanceOf[Class[_ <: LinkitPlugin]]
    }

    private def ensureContained(file: FileAdapter): Unit = {
        if (!pluginFiles.contains(file))
            throw new IllegalArgumentException(s"Plugin url '${file.getAbsolutePath}' not handled by this ClassLoader.")
    }

}

object PluginClassLoader {
    val MainClassField = "main"
    val PropertiesFileName = "plugin.properties"
}
