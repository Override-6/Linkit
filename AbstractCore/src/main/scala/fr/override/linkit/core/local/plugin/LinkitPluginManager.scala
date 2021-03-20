/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.core.local.plugin

import fr.`override`.linkit.api.local.plugin.fragment.FragmentManager
import fr.`override`.linkit.api.local.plugin.{Plugin, PluginLoader, PluginManager}
import fr.`override`.linkit.api.local.system.fsa.FileSystemAdapter
import fr.`override`.linkit.core.local.plugin.fragment.SimpleFragmentManager
import fr.`override`.linkit.core.local.system.ContextLogger

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class LinkitPluginManager(fsa: FileSystemAdapter) extends PluginManager {
    private val extractor = new LinkitPluginExtractor(fsa)
    private val plugins = ListBuffer.empty[Plugin]
    override val fragmentManager: FragmentManager = new SimpleFragmentManager

    override def load(file: String): Plugin = {
        enablePlugins(extractor.extract(file)).head
    }

    override def loadAll(folder: String): Array[Plugin] = {
        enablePlugins(extractor.extractAll(folder))
    }

    override def load(clazz: Class[_ <: Plugin]): Plugin = {
        enablePlugins(extractor.extract(clazz)).head
    }

    override def loadAll(classes: Class[_ <: Plugin]*): Array[Plugin] = {
        enablePlugins(extractor.extractAll(classes: _*))
    }

    override def countPlugins: Int = plugins.length


    private def enablePlugins(loader: PluginLoader): Array[Plugin] = {
        val buffer = new Array[Plugin](loader.length)
        for (i <- buffer.indices) {
            buffer(i) = loader.nextPlugin()
        }
        def pluginName(implicit plugin: Plugin): String = plugin.getClass.getSimpleName

        def perform(action: Plugin => Unit): Unit = buffer.foreach(implicit plugin => {
            try {
                action(plugin)
            } catch {
                case NonFatal(e) =>
                    ContextLogger.error(s"Could not load '${pluginName}'" + e.getMessage)
                    e.printStackTrace()
            }
        })
        val count = buffer.length
        ContextLogger.trace(s"Loading $count plugins...")
        perform(implicit plugin => {
            ContextLogger.trace(s"Loading ${pluginName}...")
            plugin.onLoad()
        })

        ContextLogger.trace(s"Enabling $count plugins...")
        perform(implicit plugin => {
            ContextLogger.trace(s"Enabling ${pluginName}...")
            plugin.onEnable()
        })

        plugins ++= buffer
        buffer
    }

}
