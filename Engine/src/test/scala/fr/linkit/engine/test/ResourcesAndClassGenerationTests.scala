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

import fr.linkit.api.connection.cache.repo.Puppeteer
import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.generation.PuppeteerDescription
import fr.linkit.api.local.resource.external.{ResourceFile, ResourceFolder}
import fr.linkit.api.local.system.Version
import fr.linkit.api.local.system.config.ApplicationConfiguration
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.api.local.system.security.ApplicationSecurityManager
import fr.linkit.engine.connection.cache.repo.SimplePuppeteer
import fr.linkit.engine.connection.cache.repo.generation.{ByteCodeGenericParameterTranslator, PuppetWrapperClassGenerator, WrappersClassResource}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.resource.external.LocalResourceFolder._
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.test.ResourcesAndClassGenerationTests.TestClass
import fr.linkit.engine.test.objects.PlayerObject
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[OrderAnnotation])
class ResourcesAndClassGenerationTests {

    private var resources: ResourceFolder = _

    @BeforeAll
    def init(): Unit = {
        LinkitApplication.mapEnvironment(LocalFileSystemAdapters.Nio, Seq(getClass))
    }

    @Test
    @Order(-1)
    def genericParameterTests(): Unit = {
        val expression = "<EFFE:Ljava/lang/Object;GUU:TEFFE;S:TGUU;U:Lscala/collection/immutable/List<TEFFE;>;V:Ljava/lang/Object;>"
        val result = ByteCodeGenericParameterTranslator.toJavaDeclaration(expression)
        println(s"result = ${result}")
    }

    @Test
    @Order(0)
    def methodsSignatureTests(): Unit = {
        val clazz = classOf[TestClass]
        println(s"clazz = ${clazz}")
        val methods = clazz.getDeclaredMethods
        methods.foreach { method =>
            val args = method.getGenericParameterTypes
            println(s"args = ${args.mkString("Array(", ", ", ")")}")
        }
        println("qsd")
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
        val puppetClass = generator.getClass(classOf[PlayerObject])
        println(s"puppetClass = ${puppetClass}")
        val player = PlayerObject(7, "sheeeesh", "slt", 1, 5)
        val pup    = new SimplePuppeteer[PlayerObject](null, null, PuppeteerDescription("", 8, "", Array(1)), PuppetDescription(classOf[PlayerObject]))
        val puppet = puppetClass.getDeclaredConstructor(classOf[Puppeteer[_]], classOf[PlayerObject]).newInstance(pup, player)
        println(s"puppet = ${puppet}")
    }

}

object ResourcesAndClassGenerationTests {
    class TestClass {

        def genericMethod2[EFFE <: ResourceFolder, S <: ResourceFile](t: EFFE): EFFE = t

        def genericMethod[EFFE >: ResourceFolder, I >: ResourceFolder](t: EFFE): EFFE = t
    }
}
