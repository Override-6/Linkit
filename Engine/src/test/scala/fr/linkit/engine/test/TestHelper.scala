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

package fr.linkit.engine.test

import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.system.config.ApplicationConfiguration
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.api.local.system.security.ApplicationSecurityManager
import fr.linkit.api.local.system.{AppLogger, Version}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import org.mockito.Mockito

object TestHelper {

    private var resources: ResourceFolder    = _
    private val app      : LinkitApplication = Mockito.mock(classOf[LinkitApplication])

    def initMinimalLinkit(): Unit = {
        val config      = new ApplicationConfiguration {
            override val pluginFolder   : Option[String]             = None
            override val resourceFolder : String                     = System.getenv("LinkitHome")
            override val fsAdapter      : FileSystemAdapter          = LocalFileSystemAdapters.Nio
            override val securityManager: ApplicationSecurityManager = null
        }
        val testVersion = Version("Tests", "0.0.0", false)
        System.setProperty("LinkitImplementationVersion", testVersion.toString)

        resources = LinkitApplication.prepareApplication(testVersion, config, Seq(getClass))
        Mockito.when(app.getAppResources).thenReturn(resources)
        Mockito.when(app.compilerCenter).thenReturn(new DefaultCompilerCenter)
        AppLogger.useVerbose = true
    }
}
