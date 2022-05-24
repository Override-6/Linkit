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

package fr.linkit.server.test

/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

import fr.linkit.api.internal.system.AppLoggers
import fr.linkit.api.test.HierarchyRaiserOrderer
import fr.linkit.api.test.TestUtils._
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[HierarchyRaiserOrderer])
object ServerTests {

    private val DefaultServerID = "TestServer1"
    var application: ServerApplication = _

    @Test
    def launchApplication(): Unit = {
        //AppLogger.info(s"Running server with arguments '${args.mkString(" ")}'")

        //val userDefinedPluginFolder = getOrElse(args, "--plugin-path", "/Plugins")
        //val resourcesFolder = getDefaultLinkitHome

        val config = new ServerApplicationConfigBuilder {
            override val resourcesFolder: String = resourcesFolder
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
        AppLoggers.App.trace(s"Launch complete: $application")

        Assertions.assertAll("App launch conclusion",
            Assertions.assertNotNull(application)
        )
    }

    @AfterAll
    def sleep(): Unit = {
        //Thread.currentThread().setDaemon(true)
        AppLoggers.App.trace("Sleeping...")
        Thread.sleep(9999999999999L)
    }
}
