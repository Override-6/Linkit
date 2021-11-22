/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.application.plugin

import fr.linkit.api.application.plugin.{LinkitPlugin, Plugin, PluginLoadException}
import fr.linkit.api.internal.system.AppException
import fr.linkit.engine.application.plugin.PluginClassLoader.{MainClassField, PropertiesFileName}
import fr.linkit.engine.internal.mapping.ClassMappings

import java.net.URLClassLoader
import java.nio.file.Path
import java.util.Properties
import java.util.zip.ZipFile
import scala.collection.mutable

class PluginClassLoader(private[plugin] val pluginFiles: Array[Path], bridge: PluginClassLoaderBridge)
        extends URLClassLoader(pluginFiles.map(_.toUri.toURL)) {

    private val map = mutable.HashMap.empty[Path, String]

    private[plugin] def setPluginName(file: Path, name: String): Unit = {
        ensureContained(file)
        map.put(file, name)
    }

    override def loadClass(name: String, resolve: Boolean): Class[_] = {
        val clazz = try {
            super.loadClass(name, resolve)
        } catch {
            case e: ClassNotFoundException =>
                bridge.loadClass(name, this)
        }
        ClassMappings.putClass(clazz)
        clazz
    }

    def loadMainClass(file: Path): Class[_ <: Plugin] = {
        ensureContained(file)
        loadJar(file)
    }

    def countUrls: Int = pluginFiles.length

    private def loadJar(adapter: Path): Class[_ <: Plugin] = {

        val jarFile      = new ZipFile(adapter.toFile)
        val propertyFile = jarFile.getEntry(PropertiesFileName)
        //Checking property presence
        if (propertyFile == null)
            throw PluginLoadException(s"Jar file $adapter must have a file called '$PropertiesFileName' in its root")

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
            throw new AppException(s"Class '$clazz' must extends '${classOf[LinkitPlugin]}' to be loaded as an extension.")
        }
        clazz.asInstanceOf[Class[_ <: LinkitPlugin]]
    }

    private def ensureContained(file: Path): Unit = {
        if (!pluginFiles.contains(file))
            throw new IllegalArgumentException(s"Plugin url '${file}' not handled by this ClassLoader.")
    }

}

object PluginClassLoader {

    val MainClassField     = "main"
    val PropertiesFileName = "plugin.properties"
}
