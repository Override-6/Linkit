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

import java.util

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.behavior.annotation.FieldControl
import fr.linkit.api.connection.cache.obj.behavior.{SynchronizedObjectBehavior, SynchronizedObjectBehaviorStore}
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperInstantiator
import fr.linkit.api.connection.cache.obj.invokation.InvocationChoreographer
import fr.linkit.api.local.generation.TypeVariableTranslator
import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.system.config.ApplicationConfiguration
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.api.local.system.security.ApplicationSecurityManager
import fr.linkit.api.local.system.{AppLogger, Version}
import fr.linkit.engine.connection.cache.obj.behavior.{AnnotationBasedMemberBehaviorFactory, DefaultSynchronizedObjectBehavior, SynchronizedObjectDefaultBehaviorCenter}
import fr.linkit.engine.connection.cache.obj.description.SyncObjectClassDescription
import fr.linkit.engine.connection.cache.obj.generation.{DefaultObjectWrapperClassCenter, SyncObjectClassResource, SyncObjectInstantiationHelper}
import fr.linkit.engine.connection.cache.obj.invokation.remote.ObjectPuppeteer
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.local.resource.external.LocalResourceFolder._
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.test.classes.{Player, ScalaClass, Vector2}
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._
import org.mockito.Mockito

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
        val tree = new SynchronizedObjectDefaultBehaviorCenter(AnnotationBasedMemberBehaviorFactory)
        val bhv  = DefaultSynchronizedObjectBehavior[TestClass](SyncObjectClassDescription(classOf[TestClass]), tree, null, null, null)
        println(s"bhv = ${bhv}")
    }

    private class TestClass {
        @FieldControl()
        private val test: String = "salut"
    }

    /*class FakeSynchronizedObjectPersistor extends SynchronizedObjectPersistor(null) {
        override protected def initialiseWrapper(detachedWrapper: SynchronizedObjectPersistor.DetachedWrapper): SynchronizedObject[_] = {
            println("Faking wrapper...")
            forObject(detachedWrapper.detached)
        }
    }*/

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
    }

    def forObject[A <: AnyRef : TypeTag](obj: A, tree: SynchronizedObjectBehaviorStore = new SynchronizedObjectDefaultBehaviorCenter(AnnotationBasedMemberBehaviorFactory)): A with SynchronizedObject[A] = {
        Assertions.assertNotNull(resources)

        val info         = SyncNodeInfo("", 8, "", Array(1))
        val (wrapper, _) = TestWrapperInstantiator.newWrapper[A](obj, tree, info, Map())
        wrapper.getChoreographer.forceLocalInvocation {
            println(s"wrapper = ${wrapper}")
            println(s"wrapper.getWrappedClass = ${wrapper.getSuperClass}")
        }
        wrapper
    }

    private object TestWrapperInstantiator extends ObjectWrapperInstantiator {

        private val resource  = resources.getOrOpenThenRepresent[SyncObjectClassResource](LinkitApplication.getProperty("compilation.working_dir.classes"))
        private val generator = new DefaultObjectWrapperClassCenter(new DefaultCompilerCenter, resource)

        override def newWrapper[A <: AnyRef](obj: A, behaviorTree: SynchronizedObjectBehaviorStore, puppeteerInfo: SyncNodeInfo, subWrappers: Map[AnyRef, SyncNodeInfo]): (A with SynchronizedObject[A], Map[AnyRef, SynchronizedObject[AnyRef]]) = {
            val cl                     = obj.getClass.asInstanceOf[Class[A]]
            val behaviorDesc           = behaviorTree.getFromClass[A](cl)
            val puppetClass            = generator.getWrapperClass[A](SyncObjectClassDescription[A](cl))
            val pup                    = new ObjectPuppeteer[A](null, app, null, puppeteerInfo, behaviorDesc)
            val helper                 = new SyncObjectInstantiationHelper(this, behaviorTree)
            val (wrapper, subWrappers) = helper.instantiateFromOrigin[A](puppetClass, obj, Map())
            wrapper.initPuppeteer(pup)
            (wrapper, subWrappers)
        }

        override def initializeWrapper[B <: AnyRef](wrapper: SynchronizedObject[B], nodeInfo: SyncNodeInfo, behavior: SynchronizedObjectBehavior[B]): Unit = ???
    }

}

