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
import fr.linkit.api.local.system.Version
import fr.linkit.api.local.system.config.ApplicationConfiguration
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.api.local.system.security.ApplicationSecurityManager
import fr.linkit.engine.connection.network.cache.repo.generation.{PuppetWrapperClassGenerator, WrappersClassResource}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.resource.external.LocalResourceFolder._
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.local.system.fsa.nio.NIOFileAdapter
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[OrderAnnotation])
class ResourcesAndClassGenerationTests {

    private var resources: ResourceFolder = _

    @Test
    def sbTests(): Unit = {

    }

    @Test
    @Order(1)
    def loadResources(): Unit = {
        val config = new ApplicationConfiguration {
            override val pluginFolder   : Option[String]             = None
            override val resourceFolder : String                     = "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home"
            override val fsAdapter      : FileSystemAdapter          = LocalFileSystemAdapters.Nio
            override val securityManager: ApplicationSecurityManager = null
        }
        System.setProperty("LinkitImplementationVersion", Version("Tests", "0.0.0", false).toString)
        resources = LinkitApplication.prepareAppResources(config)
    }

    @Test
    @Order(2)
    def generateClass(): Unit = {
        Assertions.assertNotNull(resources)
        val resource    = resources.getOrOpenShort[WrappersClassResource]("PuppetGeneration")
        val generator   = new PuppetWrapperClassGenerator(resource)
        val puppetClass = generator.getClass(classOf[NIOFileAdapter])
        println(s"puppetClass = ${puppetClass}")
    }

}
