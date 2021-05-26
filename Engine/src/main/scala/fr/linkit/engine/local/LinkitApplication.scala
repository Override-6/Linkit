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
import fr.linkit.api.local.concurrency.{AsyncTask, workerExecution}
import fr.linkit.api.local.plugin.PluginManager
import fr.linkit.api.local.resource.external.{LocalExternalFolder, ResourceFolder}
import fr.linkit.api.local.system.config.ApplicationConfiguration
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.api.local.system.{ApiConstants, AppException, AppLogger, Version}
import fr.linkit.engine.connection.cache.repo.generation.{PuppetWrapperClassGenerator, WrappersClassResource}
import fr.linkit.engine.local.LinkitApplication.setInstance
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.mapping.ClassMapEngine
import fr.linkit.engine.local.plugin.LinkitPluginManager
import fr.linkit.engine.local.resource.external.{LocalResourceFactories, LocalResourceFolder}
import fr.linkit.engine.local.resource.{ResourceFolderMaintainer, SimpleResourceListener}
import fr.linkit.engine.local.system.EngineConstants
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.local.system.fsa.io.{IOFileAdapter, IOFileSystemAdapter}
import fr.linkit.engine.local.system.fsa.nio.{NIOFileAdapter, NIOFileSystemAdapter}

import java.util.Objects
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

abstract class LinkitApplication(configuration: ApplicationConfiguration, appResources: ResourceFolder) extends ApplicationContext {

    override val pluginManager   : PluginManager = new LinkitPluginManager(this, configuration.fsAdapter)
    @volatile protected var alive: Boolean       = false
    protected val appPool: BusyWorkerPool

    setInstance(this)

    override def getAppResources: ResourceFolder = appResources

    protected def ensureAlive(): Unit = {
        if (!alive)
            throw new IllegalStateException("Server Application is shutdown.")
    }

    override def runLaterControl[A](task: => A): AsyncTask[A] = appPool.runLaterControl(task)

    override def runLater(task: => Unit): Unit = appPool.runLater(task)

    override def isAlive: Boolean = alive

    protected def preShutdown(): Unit = {
        wrapCloseAction("Plugin management") {
            pluginManager.close()
        }
        wrapCloseAction("Resource listener") {
            AppLogger.error("REMEMBER TO CLOSE RESOURCE LISTENER IN A FUTURE UPDATE WIH A DEDICATED CLASS FOR APP RESOURCES ROOT")
            //resourceListener.close()
        }
        wrapCloseAction("Resource management") {
            appResources.close()
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

    @workerExecution
    protected def start(): Unit = {
        appPool.ensureCurrentThreadOwned("Start must be performed into Application's pool")
        if (alive) {
            throw new AppException("Client is already started")
        }
        alive = true
        val pluginFolder = configuration.pluginFolder match {
            case Some(path) =>
                val adapter = configuration.fsAdapter.getAdapter(path)
                adapter.getAbsolutePath //converting to absolute path.
            case None       => null
        }
        if (pluginFolder != null) {
            val pluginCount = pluginManager.loadAll(pluginFolder).length
            configuration.fsAdapter.getAdapter(pluginFolder)
            AppLogger.trace(s"Loaded $pluginCount plugins from main plugin folder $pluginFolder")
        }
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

    def prepareApplication(implVersion: Version, configuration: ApplicationConfiguration, otherSources: Seq[Class[_]]): ResourceFolder = this.synchronized {
        isPrepared = true

        System.setProperty(EngineConstants.ImplVersionProperty, implVersion.toString)

        AppLogger.info("-------------------------- Linkit Framework --------------------------")
        AppLogger.info(s"\tApi Version            | ${ApiConstants.Version}")
        AppLogger.info(s"\tEngine Version         | ${EngineConstants.Version}")
        AppLogger.info(s"\tImplementation Version | ${implVersion}")
        AppLogger.info(s"\tCurrent JDK Version    | ${System.getProperty("java.version")}")

        AppLogger.info("Mapping classes, this task may take a time.")

        mapEnvironment(configuration.fsAdapter, otherSources)

        import LocalResourceFolder._

        val appResources = prepareAppResources(configuration)
        val resource     = appResources.getOrOpenShort[WrappersClassResource]("/PuppetGeneration/")
        val generator    = new PuppetWrapperClassGenerator(resource)

        generator.preGenerateClasses(classOf[LinkitApplication].getClassLoader, Seq(classOf[NIOFileAdapter], classOf[NIOFileSystemAdapter], classOf[IOFileAdapter], classOf[IOFileSystemAdapter]))
        appResources
    }

    def mapEnvironment(fsa: FileSystemAdapter, otherSources: Seq[Class[_]]): Unit = {
        ClassMapEngine.mapAllSourcesOfClasses(fsa, Seq(getClass, ClassMapEngine.getClass, Predef.getClass, classOf[ApplicationContext]))
        ClassMapEngine.mapJDK(fsa)
        ClassMapEngine.mapAllSourcesOfClasses(fsa, otherSources)
        LocalFileSystemAdapters.Nio.clearAdapters()
        LocalFileSystemAdapters.Io.clearAdapters()
    }

    def prepareAppResources(configuration: ApplicationConfiguration): ResourceFolder = {
        AppLogger.trace("Loading app resources...")
        val resourceListener = new SimpleResourceListener()
        resourceListener.startWatchService()
        val fsa         = configuration.fsAdapter
        val rootAdapter = fsa.getAdapter(Objects.requireNonNull(configuration.resourceFolder, "provided null resource folder"))

        val root = LocalResourceFolder(
            adapter = rootAdapter,
            listener = resourceListener,
            parent = null
        )
        recursiveScan(root)

        def recursiveScan(folder: LocalExternalFolder): Unit = {
            folder.scanFiles(folder.register(_, LocalResourceFactories.file))

            val subPaths = fsa.list(folder.getAdapter)
            subPaths.foreach { sub =>
                val subName = sub.getName
                if (sub.isDirectory && subPaths.exists(_.getName == ResourceFolderMaintainer.MaintainerFileName)) {
                    if (folder.isKnown(subName))
                        recursiveScan(folder.get[LocalExternalFolder](subName))
                }
            }
        }

        AppLogger.trace("App resources successfully loaded.")
        root
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
            case Success(_) =>
        }

        Runtime.getRuntime.halt(code)
        instance = null
    }

    Runtime.getRuntime.addShutdownHook(new Thread(() => exitApplication(0)))

}
