/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.local

import fr.linkit.api.local.ApplicationContext
import fr.linkit.api.local.concurrency.{AsyncTaskFuture, workerExecution}
import fr.linkit.api.local.plugin.PluginManager
import fr.linkit.api.local.resource.external.{LocalExternalFolder, ResourceFolder}
import fr.linkit.api.local.system.config.ApplicationConfiguration
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.api.local.system.{ApiConstants, AppLogger, Version}
import fr.linkit.engine.connection.network.cache.puppet.generation.PuppetWrapperClassGenerator
import fr.linkit.engine.local.LinkitApplication.setInstance
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.mapping.ClassMapEngine
import fr.linkit.engine.local.plugin.LinkitPluginManager
import fr.linkit.engine.local.resource.SimpleResourceListener
import fr.linkit.engine.local.resource.local.{LocalResourceFactories, LocalResourceFolder}
import fr.linkit.engine.local.system.EngineConstants
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.local.system.fsa.io.{IOFileAdapter, IOFileSystemAdapter}
import fr.linkit.engine.local.system.fsa.nio.{NIOFileAdapter, NIOFileSystemAdapter}

import java.util.concurrent.locks.LockSupport
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

abstract class LinkitApplication(configuration: ApplicationConfiguration) extends ApplicationContext {

    protected val resourceListener                = new SimpleResourceListener(configuration.resourceFolder)
    protected val resources      : ResourceFolder = prepareAppResources()
    override  val pluginManager  : PluginManager  = new LinkitPluginManager(this, configuration.fsAdapter)
    @volatile protected var alive: Boolean        = false
    protected val appPool: BusyWorkerPool

    setInstance(this)

    override def getAppResources: ResourceFolder = resources

    protected def ensureAlive(): Unit = {
        if (!alive)
            throw new IllegalStateException("Server Application is shutdown.")
    }

    override def runLaterControl[A](task: => A): AsyncTaskFuture[A] = appPool.runLaterControl(task)

    override def runLater(task: => Unit): Unit = appPool.runLater(task)

    override def isAlive: Boolean = alive

    protected def preShutdown(): Unit = {
        wrapCloseAction("Plugin management") {
            pluginManager.close()
        }
        wrapCloseAction("Resource listener") {
            resourceListener.close()
        }
        wrapCloseAction("Resource management") {
            resources.close()
        }
    }

    protected def wrapCloseAction(closedEntity: String)(closeAction: => Unit): Unit = {
        try {
            closeAction
        } catch {
            case NonFatal(e) =>
                Console.err.println("-----------------------------------------------------------")
                Console.err.println("    An exception has occurred during application shutdown.")
                Console.err.println(s"This exception has been thrown while closing $closedEntity")
                Console.err.println("-----------------------------------------------------------")
                e.printStackTrace()
        }
    }

    private def prepareAppResources(): ResourceFolder = {
        AppLogger.trace("Loading app resources...")
        resourceListener.startWatchService()
        val fsa         = configuration.fsAdapter
        val rootAdapter = fsa.getAdapter(configuration.resourceFolder)

        val root = LocalResourceFolder(
            adapter = rootAdapter,
            listener = resourceListener,
            parent = null
        )
        recursiveScan(root)

        def recursiveScan(folder: LocalExternalFolder): Unit = {
            folder.scan(folder.register(_, LocalResourceFactories.adaptive))

            fsa.list(folder.getAdapter).foreach { sub =>
                if (sub.isDirectory) {
                    recursiveScan(folder.get[LocalExternalFolder](sub.getName))
                }
            }
        }

        AppLogger.trace("App resources successfully loaded.")
        root
    }

}

object LinkitApplication {

    @volatile private var instance  : LinkitApplication = _
    @volatile private var isPrepared: Boolean           = false

    private def setInstance(instance: LinkitApplication): Unit = this.synchronized {
        if (this.instance != null)
            throw new IllegalAccessError("Only one LinkitApplication per Java process is permitted.")
        if (!isPrepared)
            throw new IllegalStateException("Application must be prepared before any launch. Please use LinkitApplication.prepareApplication.")
        this.instance = instance
    }

    def prepareApplication(implVersion: Version, fsa: FileSystemAdapter, otherSources: Seq[Class[_]]): Unit = this.synchronized {
        isPrepared = true

        System.setProperty(EngineConstants.ImplVersionProperty, implVersion.toString)

        AppLogger.info("-------------------------- Linkit Framework --------------------------")
        AppLogger.info(s"\tApi Version            | ${ApiConstants.Version}")
        AppLogger.info(s"\tEngine Version   | ${EngineConstants.Version}")
        AppLogger.info(s"\tImplementation Version | ${implVersion}")
        AppLogger.info(s"\tCurrent JDK Version    | ${System.getProperty("java.version")}")

        AppLogger.info("Mapping classes, this task may take a time.")

        ClassMapEngine.mapAllSourcesOfClasses(fsa, otherSources)
        ClassMapEngine.mapAllSourcesOfClasses(fsa, Seq(getClass, ClassMapEngine.getClass, Predef.getClass, classOf[ApplicationContext]))
        ClassMapEngine.mapJDK(fsa)
        LocalFileSystemAdapters.Nio.clearAdapters()
        LocalFileSystemAdapters.Io.clearAdapters()

        PuppetWrapperClassGenerator.getOrGenerate(classOf[NIOFileSystemAdapter])
        PuppetWrapperClassGenerator.getOrGenerate(classOf[NIOFileAdapter])
        PuppetWrapperClassGenerator.getOrGenerate(classOf[IOFileSystemAdapter])
        PuppetWrapperClassGenerator.getOrGenerate(classOf[IOFileAdapter])
    }

    @workerExecution
    def exitApplication(code: Int): Unit = this.synchronized {
        if (instance == null) {
            Runtime.getRuntime.halt(code)
            return
        }

        AppLogger.info("Exiting application...")
        instance.runLaterControl {
            instance.preShutdown()
            instance.shutdown()
        }.join() match {
            case Failure(e) => e.printStackTrace()
            case Success(_)     =>
        }

        Runtime.getRuntime.halt(code)
        instance = null
    }

    Runtime.getRuntime.addShutdownHook(new Thread(() => exitApplication(0)))

}
