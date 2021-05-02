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

package fr.linkit.client.test

import fr.linkit.api.local.plugin.Plugin
import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.test.HierarchyRaiserOrderer
import fr.linkit.api.test.TestUtils._
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}
import fr.linkit.engine.test.EngineTests
import fr.linkit.plugin.controller.ControllerExtension
import fr.linkit.plugin.debug.DebugExtension
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance, TestMethodOrder}

import java.io.File
import java.net.InetSocketAddress

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[HierarchyRaiserOrderer])
class ClientLauncher extends EngineTests {

    val Port           : Int               = 48484
    val Localhost      : InetSocketAddress = new InetSocketAddress("localhost", Port)
    val HomeProperty   : String            = "LinkitHome"
    val DefaultHomePath: String            = System.getenv("LOCALAPPDATA") + s"${File.separator}Linkit${File.separator}"

    var application: ClientApplication = _

    @Test
    def launch(): Unit = {
        val config = new ClientApplicationConfigBuilder {
            override val resourcesFolder: String = getDefaultLinkitHome
            nWorkerThreadFunction = _ + 1
            loadSchematic = new ScalaClientAppSchematic {
                clients += new ClientConnectionConfigBuilder {
                    pluginFolder = None // Some(mainPluginFolder)
                    override val identifier   : String            = "TestClient"
                    override val remoteAddress: InetSocketAddress = Localhost
                }
            }
        }
        application = Assertions.assertDoesNotThrow {
            ClientApplication.launch(config, getClass)
        }
        AppLogger.info(s"Launch complete: $application")

        Assertions.assertAll("App launch conclusion",
            Assertions.assertNotNull(application)
        )
        EngineTests.application = application
    }

    @Test
    def plugins(): Unit = {
        if (application == null)
            fail()

        application.runLaterControl {
            val pluginManager = application.pluginManager
            pluginManager.loadAllClass(Array(
                classOf[ControllerExtension]: Class[_ <: Plugin],
                classOf[DebugExtension]: Class[_ <: Plugin],
            ))
        }.join()

        AppLogger.info("Server Application ready to use.")
    }

}
