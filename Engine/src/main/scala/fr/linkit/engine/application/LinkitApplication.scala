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

package fr.linkit.engine.application

import fr.linkit.api.application.config.ApplicationConfiguration
import fr.linkit.api.application.resource.external.{LocalFolder, ResourceFolder}
import fr.linkit.api.application.{ApplicationContext, ApplicationReference}
import fr.linkit.api.internal.concurrency.{AsyncTask, workerExecution}
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.api.internal.system.{ApiConstants, AppException, AppLoggers, Version}
import fr.linkit.engine.application.LinkitApplication.setInstance
import fr.linkit.engine.application.resource.external.{LocalResourceFactories, LocalResourceFile, LocalResourceFolder}
import fr.linkit.engine.application.resource.{ResourceFolderMaintainer, SimpleResourceListener}
import fr.linkit.engine.internal.concurrency.pool.AbstractWorkerPool
import fr.linkit.engine.internal.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.language.bhv.Contract
import fr.linkit.engine.internal.mapping.{ClassMappings, MappingEngine}
import fr.linkit.engine.internal.system.{EngineConstants, InternalLibrariesLoader}

import java.nio.file.{Files, Path}
import java.util.{Objects, Properties}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

abstract class LinkitApplication(configuration: ApplicationConfiguration, appResources: ResourceFolder) extends ApplicationContext {

    override val compilerCenter  : CompilerCenter = new DefaultCompilerCenter
    @volatile protected var alive: Boolean        = false
    protected val appPool: AbstractWorkerPool
    override val reference: ApplicationReference = ApplicationReference

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
        wrapCloseAction("Resource listener") {
            AppLoggers.App.error("REMEMBER TO CLOSE RESOURCE LISTENER IN A FUTURE UPDATE WIH A DEDICATED CLASS FOR APP RESOURCES ROOT")
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
        AppLoggers.App.info("Parsing found behavior contracts...")
        Contract.precompute(this)
    }

}

object LinkitApplication {

    private val AppPropertiesName     = "app.properties"
    private val AppDefaultsProperties = "/app_defaults.properties"
    private val LibrariesNames        = Array("LinkitNativesHelper")

    private val properties          : Properties        = new Properties()
    @volatile private var instance  : LinkitApplication = _
    @volatile private var isPrepared: Boolean           = false

    private def setInstance(instance: LinkitApplication): Unit = this.synchronized {
        if (this.instance != null)
            throw new IllegalAccessException("Only one LinkitApplication per JVM process is permitted.")
        if (!isPrepared)
            throw new IllegalStateException("LinkitApplication must be prepared before any launch. Please use LinkitApplication.prepareApplication.")
        this.instance = instance
    }

    def getProperty(name: String): String = {
        properties.getProperty(name)
    }

    def getHomePath(path: String): Path = {
        Path.of(instance.getAppResources.getPath + "/" + path)
    }

    def getPathProperty(name: String): Path = getHomePath(getProperty(name))

    def setProperty(name: String, property: String): String = properties.setProperty(name, property) match {
        case str: String => str
        case null        => null
        case obj         => obj.toString
    }

    def saveProperties(): Unit = {
        val propertiesResource = instance.getAppResources.get[LocalResourceFile](AppPropertiesName)
        properties.store(Files.newOutputStream(propertiesResource.getPath), null)
    }

    def prepareApplication(implVersion: Version, configuration: ApplicationConfiguration, otherSources: Seq[Class[_]]): ResourceFolder = this.synchronized {

        System.setProperty(EngineConstants.ImplVersionProperty, implVersion.toString)
    
        AppLoggers.App.info("-------------------------- Linkit Framework --------------------------")
        AppLoggers.App.info(s"\tApi Version            | ${ApiConstants.Version}")
        AppLoggers.App.info(s"\tEngine Version         | ${EngineConstants.Version}")
        AppLoggers.App.info(s"\tImplementation Version | ${implVersion}")
        AppLoggers.App.info(s"\tCurrent JDK Version    | ${System.getProperty("java.version")}")
    
        mapEnvironment(otherSources)

        val appResources        = prepareAppResources(configuration)
        val propertiesResources = appResources.find[LocalResourceFile](AppPropertiesName)
            .getOrElse {
                val res = appResources.openResource(AppPropertiesName, LocalResourceFile)
                Files.write(res.getPath, getClass.getResourceAsStream(AppDefaultsProperties).readAllBytes())
                res
            }
        AppLoggers.App.info("Loading properties...")
        properties.load(Files.newInputStream(propertiesResources.getPath))
        AppLoggers.App.info("Loading Native Libraries...")
        InternalLibrariesLoader.extractAndLoad(appResources, LibrariesNames)

        isPrepared = true
        appResources
    }

    def mapEnvironment(otherSources: Seq[Class[_]]): Unit = {
        AppLoggers.App.info("Mapping classes...")
        MappingEngine.mapAllSourcesOfClasses(Seq(getClass, MappingEngine.getClass, Predef.getClass, classOf[ApplicationContext]))
        MappingEngine.mapJDK()
        MappingEngine.mapAllSourcesOfClasses(otherSources)
        AppLoggers.Mappings.info(s"Environment mapped: mapped total of ${ClassMappings.classCount} classes.")
    }

    private def prepareAppResources(configuration: ApplicationConfiguration): ResourceFolder = {
        AppLoggers.App.trace("Loading app resources...")
        val resourceListener = new SimpleResourceListener()
        resourceListener.startWatchService()
        val rootPath = Path.of(Objects.requireNonNull(configuration.resourceFolder, "provided null resource folder"))

        val root = LocalResourceFolder(
            path = rootPath,
            listener = resourceListener,
            parent = null
        )
        recursiveScan(root)

        def recursiveScan(folder: LocalFolder): Unit = {
            folder.scanFiles(folder.register(_, LocalResourceFactories.file))

            val subPaths = Files.list(folder.getPath).toArray(new Array[Path](_))
            subPaths.foreach { sub =>
                val subName = sub.getFileName.toString
                if (Files.isDirectory(sub) && subPaths.exists(_.getFileName.toString == ResourceFolderMaintainer.MaintainerFileName)) {
                    if (folder.isKnown(subName))
                        recursiveScan(folder.get[LocalFolder](subName))
                }
            }
        }
    
        AppLoggers.App.trace("App resources successfully loaded.")
        root
    }

    @workerExecution
    def exitApplication(code: Int): Unit = this.synchronized {
        if (instance == null) {
            Runtime.getRuntime.halt(code)
            return
        }
    
        AppLoggers.App.info("Exiting application...")
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
