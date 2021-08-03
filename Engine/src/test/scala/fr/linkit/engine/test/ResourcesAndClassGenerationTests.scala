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

import fr.linkit.api.connection.cache.obj.description.{ObjectTreeBehavior, WrapperBehavior, WrapperNodeInfo}
import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperInstantiator
import fr.linkit.api.connection.cache.obj.{InvocationChoreographer, PuppetWrapper}
import fr.linkit.api.local.generation.TypeVariableTranslator
import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.system.config.ApplicationConfiguration
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.api.local.system.security.ApplicationSecurityManager
import fr.linkit.api.local.system.{AppLogger, Version}
import fr.linkit.engine.connection.cache.obj.description.annotation.AnnotationBasedMemberBehaviorFactory
import fr.linkit.engine.connection.cache.obj.description.{ObjectTreeDefaultBehavior, SimplePuppetClassDescription}
import fr.linkit.engine.connection.cache.obj.generation.{DefaultObjectWrapperClassCenter, WrapperInstantiationHelper, WrappersClassResource}
import fr.linkit.engine.connection.cache.obj.invokation.remote.InstancePuppeteer
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.local.resource.external.LocalResourceFolder._
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.test.ScalaReflectionTests.TestClass
import fr.linkit.engine.test.classes.Player
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._
import org.mockito.Mockito

import java.util
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.reflect.runtime.universe.TypeTag

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[OrderAnnotation])
class ResourcesAndClassGenerationTests {

    private var resources: ResourceFolder    = _
    private val app      : LinkitApplication = Mockito.mock(classOf[LinkitApplication])

    @BeforeAll
    def init(): Unit = {
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
        LinkitApplication.setInstance(app)
        AppLogger.useVerbose = true
    }

    @Test
    @Order(-1)
    def genericParameterTests(): Unit = {
        val testMethod  = classOf[TestClass].getMethod("genericMethod2")
        val javaResult  = TypeVariableTranslator.toJavaDeclaration(testMethod.getTypeParameters)
        val scalaResult = TypeVariableTranslator.toScalaDeclaration(testMethod.getTypeParameters)
        println(s"Java result = ${javaResult}")
        println(s"Scala result = ${scalaResult}")
    }

    @Test
    def packetTest(): Unit = InvocationChoreographer.forceLocalInvocation {
        val wrapper = forObject(new util.ArrayList[String]())

        val packet = ArrayBuffer(wrapper, wrapper)
        PacketTests.testPacket(Array(packet))
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
    @Order(2)
    def generateSimpleClass(): Unit = InvocationChoreographer.forceLocalInvocation {
        val obj = forObject(Player(789, "salut", "wow", 8, 2))
        println(s"obj = ${obj}")
    }

    /*class FakePuppetWrapperPersistor extends PuppetWrapperPersistor(null) {
        override protected def initialiseWrapper(detachedWrapper: PuppetWrapperPersistor.DetachedWrapper): PuppetWrapper[_] = {
            println("Faking wrapper...")
            forObject(detachedWrapper.detached)
        }
    }*/

    @Test
    @Order(3)
    def generateComplexScalaClass(): Unit = InvocationChoreographer.forceLocalInvocation {
        val list = forObject(ListBuffer.empty[Player])
        val player = forObject(Player(7, "Salut", "Hey", 891, 45))
        list += player
        val detached = list.detachedClone()
        println(s"detached = ${detached}")
    }

    @Test
    @Order(3)
    def generateComplexJavaClass(): Unit = {
        forObject(LocalFileSystemAdapters.Nio)
    }

    def forObject[A <: AnyRef: TypeTag](obj: A): A with PuppetWrapper[A] = {
        Assertions.assertNotNull(resources)

        val tree    = new ObjectTreeDefaultBehavior(AnnotationBasedMemberBehaviorFactory())
        val info    = WrapperNodeInfo("", 8, "", Array(1))
        val (wrapper, _) = TestWrapperInstantiator.newWrapper[A](obj, tree, info, Map())
        wrapper.getChoreographer.forceLocalInvocation {
            println(s"wrapper = ${wrapper}")
            println(s"wrapper.getWrappedClass = ${wrapper.getWrappedClass}")
        }
        wrapper
    }

    private object TestWrapperInstantiator extends ObjectWrapperInstantiator {

        private val resource  = resources.getOrOpenThenRepresent[WrappersClassResource](LinkitApplication.getProperty("compilation.working_dir.classes"))
        private val generator = new DefaultObjectWrapperClassCenter(new DefaultCompilerCenter, resource)

        override def newWrapper[A <: AnyRef](obj: A, behaviorTree: ObjectTreeBehavior, puppeteerInfo: WrapperNodeInfo, subWrappers: Map[AnyRef, WrapperNodeInfo]): (A with PuppetWrapper[A], Map[AnyRef, PuppetWrapper[AnyRef]]) = {
            val cl                     = obj.getClass.asInstanceOf[Class[A]]
            val behaviorDesc           = behaviorTree.getFromClass[A](cl)
            val puppetClass            = generator.getWrapperClass[A](SimplePuppetClassDescription(cl))
            val pup                    = new InstancePuppeteer[A](null, app, null, puppeteerInfo, behaviorDesc)
            val helper                 = new WrapperInstantiationHelper(this, behaviorTree)
            val (wrapper, subWrappers) = helper.instantiateFromOrigin[A](puppetClass, obj, Map())
            wrapper.initPuppeteer(pup)
            (wrapper, subWrappers)
        }

        override def initializeWrapper[B <: AnyRef](wrapper: PuppetWrapper[B], nodeInfo: WrapperNodeInfo, behavior: WrapperBehavior[B]): Unit = ???
    }

}

