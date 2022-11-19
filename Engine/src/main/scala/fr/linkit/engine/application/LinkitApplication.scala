/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.application

import fr.linkit.api.application.config.ApplicationConfiguration
import fr.linkit.api.application.resource.local.{LocalFolder, ResourceFolder}
import fr.linkit.api.application.{ApplicationContext, ApplicationReference}
import fr.linkit.api.internal.compilation.CompilerCenter
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.api.internal.system.{ApiConstants, AppException, Version}
import fr.linkit.engine.application.LinkitApplication.setInstance
import fr.linkit.engine.application.resource.local.{LocalResourceFactories, LocalResourceFile, LocalResourceFolder}
import fr.linkit.engine.application.resource.{ResourceFolderMaintainer, SimpleResourceListener}
import fr.linkit.engine.gnom.persistence.ProtocolConstants
import fr.linkit.engine.internal.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.language.bhv.ContractProvider
import fr.linkit.engine.internal.mapping.{ClassMappings, MappingEngine}
import fr.linkit.engine.internal.system.{EngineConstants, InternalLibrariesLoader}

import java.nio.file.{Files, Path}
import java.util.concurrent.Future
import java.util.{Objects, Properties}
import scala.util.control.NonFatal

abstract class LinkitApplication(configuration: ApplicationConfiguration, appResources: ResourceFolder) extends ApplicationContext {

    override val compilerCenter: CompilerCenter = DefaultCompilerCenter
    protected val appPool: Procrastinator
    @volatile protected var alive: Boolean = false
    override val reference                 = ApplicationReference

    if (LinkitApplication.instance != null)
        AppLoggers.App.error(s"${getClass.getSimpleName}: Could not initialize singleton.")
    else setInstance(this)


    override def getAppResources: ResourceFolder = appResources

    protected def ensureAlive(): Unit = {
        if (!alive)
            throw new IllegalStateException("Server Application is shutdown.")
    }


    override def runLater[A](f: => A): Future[A] = appPool.runLater(f)

    override def isAlive: Boolean = alive

    protected def preShutdown(): Unit = {
        wrapCloseAction("Resource listener") {
            //AppLoggers.App.error("REMEMBER TO CLOSE RESOURCE LISTENER IN A FUTURE UPDATE WIH A DEDICATED CLASS FOR APP RESOURCES ROOT")
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

    protected[linkit] def start(): Unit = {
        if (alive) {
            throw new AppException("Application is already started")
        }
        alive = true
        AppLoggers.App.debug("Parsing found behavior contracts...")
        ContractProvider.precompute()
    }

}

object LinkitApplication {

    private val AppPropertiesName     = "app.properties"
    private val AppDefaultsProperties = "/app_defaults.properties"
    private val LibrariesNames        = Array("LinkitNatives")

    private val properties          : Properties        = new Properties()
    @volatile private var instance  : LinkitApplication = _
    @volatile private var isPrepared: Boolean           = false

    private def setInstance(instance: LinkitApplication): Unit = this.synchronized {
        if (this.instance != null) {
            if (instance ne this.instance)
                throw new IllegalAccessException("Only one LinkitApplication per JVM process is permitted.")
            else throw new IllegalStateException("Linkit Application already set !")
        }
        if (!isPrepared)
            throw new IllegalStateException("LinkitApplication must be prepared before any launch. Please use LinkitApplication.prepareApplication.")
        this.instance = instance
    }

    private[linkit] def getApplication: ApplicationContext = {
        if (instance == null) {
            throw new IllegalStateException("Application not initialised.")
        }
        instance
    }

    def getProperty(name: String): String = {
        properties.getProperty(name)
    }

    def getHomePath(path: String): Path = {
        Path.of(instance.getAppResources.getPath + "/" + path)
    }

    def getPathProperty(name: String): Path = getHomePath(getProperty(name))

    def setProperty(name    : String,
                    property: String): String = {
        val r = properties.setProperty(name, property) match {
            case str: String => str
            case null        => null
            case obj         => obj.toString
        }
        saveProperties()
        r
    }

    def saveProperties(): Unit = {
        val propertiesResource = instance.getAppResources.get[LocalResourceFile](AppPropertiesName)
        properties.store(Files.newOutputStream(propertiesResource.getPath), null)
    }

    def prepareApplication(implVersion: Version, configuration: ApplicationConfiguration, otherSources: Seq[Class[_]]): ResourceFolder = this.synchronized {

        System.setProperty(EngineConstants.ImplVersionProperty, implVersion.toString)
        configuration.logfilename.foreach(System.setProperty(AppLoggers.LogFileProperty, _))

        AppLoggers.App.info("-------------------------------------- Linkit Framework --------------------------------------")
        AppLoggers.App.info(s"\tApi Version            | ${ApiConstants.Version}")
        AppLoggers.App.info(s"\tEngine Version         | ${EngineConstants.Version} - Packet protocol version: ${ProtocolConstants.ProtocolVersion}")
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
        AppLoggers.App.debug("Loading properties...")
        properties.load(Files.newInputStream(propertiesResources.getPath))
        AppLoggers.App.info("Loading Native Libraries...")
        InternalLibrariesLoader.extractAndLoad(appResources, LibrariesNames)

        isPrepared = true
        appResources
    }

    def mapEnvironment(otherSources: Seq[Class[_]]): Unit = {
        AppLoggers.App.info("Mapping classes...")
        MappingEngine.mapAllSourcesOfClasses(Seq(getClass, MappingEngine.getClass, Predef.getClass, classOf[ApplicationContext]))
        MappingEngine.mapAllSourcesOfClasses(otherSources)
        MappingEngine.mapJDK()
        AppLoggers.Mappings.debug(s"Environment mapped: mapped total of ${ClassMappings.classCount} classes.")
    }

    private def prepareAppResources(configuration: ApplicationConfiguration): ResourceFolder = {
        AppLoggers.App.trace("Loading app resources...")
        val resourceListener = new SimpleResourceListener()
        resourceListener.startWatchService()
        val rootPath = Path.of(Objects.requireNonNull(configuration.resourceFolder, "provided null resource folder"))

        val root = LocalResourceFolder(
            path = rootPath,
            listener = resourceListener,
            parent = None
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

    def exitApplication(code: Int): Unit = this.synchronized {
        if (instance == null) {
            Runtime.getRuntime.halt(code)
            return
        }

        AppLoggers.App.info("Exiting application...")
        instance.runLater {
            instance.preShutdown()
            instance.shutdown()
        }.get() //get to wait

        instance = null
    }

    Runtime.getRuntime.addShutdownHook(new Thread(() => exitApplication(0)))

}
