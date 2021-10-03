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

package fr.linkit.engine.test

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehaviorStore
import fr.linkit.api.gnom.cache.sync.behavior.annotation.Synchronized
import fr.linkit.api.gnom.cache.sync.instantiation.{SyncInstanceInstantiator, SyncInstanceGetter}
import fr.linkit.api.gnom.cache.sync.invokation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.tree.SyncNodeReference
import fr.linkit.api.internal.generation.TypeVariableTranslator
import fr.linkit.api.application.resource.external.ResourceFolder
import fr.linkit.api.application.config.ApplicationConfiguration
import fr.linkit.api.internal.system.fsa.FileSystemAdapter
import fr.linkit.api.internal.system.security.ApplicationSecurityManager
import fr.linkit.api.internal.system.{AppLogger, Version}
import fr.linkit.engine.gnom.cache.sync.behavior.{AnnotationBasedMemberBehaviorFactory, DefaultObjectBehavior, DefaultObjectBehaviorStore}
import fr.linkit.engine.gnom.cache.sync.description.SimpleSyncObjectSuperClassDescription
import fr.linkit.engine.gnom.cache.sync.generation.{DefaultSyncClassCenter, SyncObjectClassResource}
import fr.linkit.engine.gnom.cache.sync.instantiation.ContentSwitcher
import fr.linkit.engine.gnom.cache.sync.invokation.remote.ObjectPuppeteer
import fr.linkit.engine.internal.LinkitApplication
import fr.linkit.engine.internal.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.application.resource.external.LocalResourceFolder._
import fr.linkit.engine.internal.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.test.classes.{Player, ScalaClass, Vector2}
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
        val obj = forObject(new ScalaClass)
        obj.testRMI("Test")
        println(s"obj = ${obj}")
    }

    @Test
    def behaviorTests(): Unit = {
        val tree = new DefaultObjectBehaviorStore(AnnotationBasedMemberBehaviorFactory)
        val bhv  = DefaultObjectBehavior[TestClass](SimpleSyncObjectSuperClassDescription(classOf[TestClass]), tree, null, null, null)
        println(s"bhv = ${bhv}")
    }

    private class TestClass {

        @Synchronized()
        private val test: String = "salut"
    }

    @Test
    @Order(3)
    def generateComplexScalaClass(): Unit = InvocationChoreographer.forceLocalInvocation {
        val list = forObject(ListBuffer.empty[Player])
        //val player = forObject(Player(7, "Salut", "Hey", 891, 45))
        //list += player
        //val clone = forObject(list.detachedClone())
        println(s"list = ${list}")
        list.copyToArray(new Array(2))
    }

    @Test
    @Order(3)
    def generateComplexJavaClass(): Unit = {
        val obj = forObject(new Vector2())
        obj.add(7, 2)
        obj.add(new Vector2(7, 2))
        println(s"obj = ${obj}")
        PacketTests.testPacket(Array(obj))
    }

    def forObject[A <: AnyRef : TypeTag](obj: A, tree: ObjectBehaviorStore = new DefaultObjectBehaviorStore(AnnotationBasedMemberBehaviorFactory)): A with SynchronizedObject[A] = {
        Assertions.assertNotNull(resources)

        val info         = SyncNodeReference("", 8, "", Array(1))
        val wrapper = TestWrapperInstantiator.newWrapper[A](new ContentSwitcher[A](obj))
        wrapper.getChoreographer.forceLocalInvocation {
            println(s"wrapper = ${wrapper}")
            println(s"wrapper.getWrappedClass = ${wrapper.getSuperClass}")
        }
        wrapper
    }

    private object TestWrapperInstantiator extends SyncInstanceInstantiator {

        private val resource  = resources.getOrOpenThenRepresent[SyncObjectClassResource](LinkitApplication.getProperty("compilation.working_dir.classes"))
        private val generator = new DefaultSyncClassCenter(new DefaultCompilerCenter, resource)


        override def newWrapper[A <: AnyRef](creator: SyncInstanceGetter[A]): A with SynchronizedObject[A] = {
            val cl           = creator.tpeClass
            val syncClass    = generator.getSyncClassFromDesc[A](SimpleSyncObjectSuperClassDescription[A](cl))
            val syncObject   = creator.getInstance(syncClass)
            syncObject
        }

    }

}

