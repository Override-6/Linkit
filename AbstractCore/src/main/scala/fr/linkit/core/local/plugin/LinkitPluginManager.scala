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

package fr.linkit.core.local.plugin

import fr.linkit.api.local.ApplicationContext
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.plugin.fragment.FragmentManager
import fr.linkit.api.local.plugin.{Plugin, PluginLoader, PluginManager}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.mapping.ClassMapEngine
import fr.linkit.core.local.plugin.fragment.SimpleFragmentManager

import java.nio.file.{NoSuchFileException, NotDirectoryException}
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class LinkitPluginManager(context: ApplicationContext, fsa: FileSystemAdapter) extends PluginManager {

    private val bridge  = new PluginClassLoaderBridge
    private val plugins = ListBuffer.empty[Plugin]

    override val fragmentManager: FragmentManager = new SimpleFragmentManager

    @workerExecution
    override def load(file: String): Plugin = {
        BusyWorkerPool.ensureCurrentIsWorker("Plugin loading must be performed in a worker pool")
        AppLogger.debug(s"Plugin $file is preparing to be loaded.")

        val adapter = fsa.getAdapter(file)

        if (adapter.notExists)
            throw new NoSuchFileException(file)

        val classLoader  = bridge.newClassLoader(Array(adapter))
        val pluginLoader = new URLPluginLoader(context, classLoader)
        enablePlugins(pluginLoader).head
    }

    @workerExecution
    override def loadAll(folder: String): Array[Plugin] = {
        BusyWorkerPool.ensureCurrentIsWorker("Plugin loading must be performed in a worker pool")
        AppLogger.debug(s"Plugins into folder '$folder' are preparing to be loaded.")

        val adapter = fsa.getAdapter(folder)
        if (adapter.notExists)
            throw new NoSuchFileException(folder)
        if (!adapter.isDirectory)
            throw new NotDirectoryException(folder)

        val classLoader  = bridge.newClassLoader(fsa.list(adapter))
        val pluginLoader = new URLPluginLoader(context, classLoader)
        enablePlugins(pluginLoader)
    }

    @workerExecution
    override def loadClass(clazz: Class[_ <: Plugin]): Plugin = {
        loadAllClass(Array(clazz): Array[Class[_ <: Plugin]]).head
    }

    @workerExecution
    override def loadAllClass(classes: Array[Class[_ <: Plugin]]): Array[Plugin] = {
        if (classes.isEmpty)
            return Array()

        ClassMapEngine.mapAllSourcesOfClasses(fsa, classes: _*)
        BusyWorkerPool.ensureCurrentIsWorker("Plugin loading must be performed in a worker pool")
        val pluginLoader = new DirectPluginLoader(context, classes)
        enablePlugins(pluginLoader)
    }

    override def countPlugins: Int = plugins.length

    private def enablePlugins(loader: PluginLoader): Array[Plugin] = {
        val buffer = new Array[Plugin](loader.length)
        for (i <- buffer.indices) {
            buffer(i) = loader.nextPlugin()
        }

        def pluginName(implicit plugin: Plugin): String = plugin.name

        def perform(action: Plugin => Unit): Unit = buffer.foreach(implicit plugin => {
            try {
                action(plugin)
            } catch {
                case NonFatal(e) =>
                    AppLogger.error(s"Could not load '${pluginName}'" + e.getMessage)
                    AppLogger.printStackTrace(e)
            }
        })

        AppLogger.debug(s"Loading plugins...")
        perform(implicit plugin => {
            AppLogger.debug(s"Loading ${pluginName}...")
            plugin.onLoad()
        })

        AppLogger.debug(s"Enabling plugins...")
        perform(implicit plugin => {
            AppLogger.debug(s"Enabling ${pluginName}...")
            plugin.onEnable()
        })

        plugins ++= buffer
        buffer
    }

}
