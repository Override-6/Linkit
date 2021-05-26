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

package fr.linkit.server.test

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

import fr.linkit.api.local.plugin.Plugin
import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.test.HierarchyRaiserOrderer
import fr.linkit.api.test.TestUtils._
import fr.linkit.engine.test.EngineTests
import fr.linkit.plugin.controller.ControllerExtension
import fr.linkit.plugin.debug.DebugExtension
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[HierarchyRaiserOrderer])
object ServerTests extends EngineTests {

    private val DefaultServerID = "TestServer1"
    var application: ServerApplication = _

    @Test
    def launchApplication(): Unit = {
        println(s"classOf[EngineTests] = ${classOf[EngineTests]}")
        //AppLogger.info(s"Running server with arguments '${args.mkString(" ")}'")

        //val userDefinedPluginFolder = getOrElse(args, "--plugin-path", "/Plugins")
        val resourcesFolder = getDefaultLinkitHome

        val config = new ServerApplicationConfigBuilder {
            override val resourceFolder: String = resourcesFolder
            pluginFolder = None //userDefinedPluginFolder
            mainPoolThreadCount = 2
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = DefaultServerID
                    override val port      : Int    = 48484
                    nWorkerThreadFunction = _ + 1 //Two threads per connections.

                    configName = "config1"
                }
            }
        }
        application = Assertions.assertDoesNotThrow {
            ServerApplication.launch(config, getClass)
        }
        AppLogger.info(s"Launch complete: $application")

        Assertions.assertAll("App launch conclusion",
            Assertions.assertNotNull(application)
        )
        EngineTests.application = application
    }

    @Test
    def plugins(): Unit = {
        Assertions.assertNotNull(application)

        application.runLaterControl {
            val pluginManager = application.pluginManager
            pluginManager.loadAllClass(Array(
                classOf[ControllerExtension]: Class[_ <: Plugin],
                classOf[DebugExtension]: Class[_ <: Plugin],
            ))
        }.join()

        AppLogger.info("Server Application ready to use.")
    }

    @AfterAll
    def sleep(): Unit = {
        //Thread.currentThread().setDaemon(true)
        AppLogger.debug("Sleeping...")
        Thread.sleep(9999999999999L)
    }
}
