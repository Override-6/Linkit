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

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.plugin.{Plugin, PluginLoadException, PluginLoader}

import scala.util.control.NonFatal

class URLPluginLoader(context: ApplicationContext, loader: PluginClassLoader) extends PluginLoader {

    @volatile private var count = 0

    override def nextPlugin(): Plugin = {
        val adapter   = loader.pluginFiles(count)
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
