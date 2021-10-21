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

package fr.linkit.server.test.sync

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor
import fr.linkit.engine.test.Player
import fr.linkit.engine.test.sync.SimpleSynchronizedObjectsTest
import fr.linkit.engine.test.sync.SimpleSynchronizedObjectsTest.OverrideMotherPlayer
import fr.linkit.server.test.ServerLauncher
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._

import scala.collection.mutable.ListBuffer

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[OrderAnnotation])
class ServerSideSimpleListBufferTests extends SimpleSynchronizedObjectsTest {

    override lazy val app: ApplicationContext = ServerLauncher.launch()
    private var testList : ListBuffer[Player] = _

    def before(): Unit = {
        AppLogger.info("Waiting partner...")
        waitNextStep()
    }

    def after(): Unit = {
        AppLogger.info("Next step...")
        unlockNextStep()
    }

    @Test
    @Order(0)
    def postListBuffer(): Unit = {
        testList = testCache.syncObject(1, Constructor())
        Assertions.assertNotNull(testList)
        Assertions.assertTrue(testList.isEmpty)
        AppLogger.info("Synchronised list has been created")
    }

    @Test
    @Order(1)
    def assertPlayerAdded(): Unit = {
        before()
        Assertions.assertTrue(testList.size == 1)
        val player = testList.head
        Assertions.assertNotNull(player)
        Assertions.assertTrue(player == OverrideMotherPlayer)
        Assertions.assertTrue(player ne OverrideMotherPlayer)
        Assertions.assertInstanceOf(classOf[SynchronizedObject[_]], player)
        after()
    }

    @Test
    @Order(2)
    def modifyPlayer(): Unit = {
        before()
        val player = testList.head
        val (oldX, oldY)  = (player.x, player.y)
        player.x += 78
        player.y -= 777
        Assertions.assertTrue(oldX != player.x)
        Assertions.assertTrue(oldY != player.y)
        after()
    }

    @Test
    @Order(3)
    def checkPlayerReInjected(): Unit = {
        before()
        Assertions.assertTrue(testList.size == 2)
        Assertions.assertSame(testList.head, testList.last)
        after()
    }

    @Test
    @Order(4)
    def checkPlayerReInjected100x(): Unit = {
        before()
        Assertions.assertTrue(testList.size == 102)
        testList.foreach(p => Assertions.assertSame(p, testList.head))
        after()
    }

    @Test
    @Order(5)
    def checkRandomPlayerAdded100x(): Unit = {
        before()
        Assertions.assertTrue(testList.size == 202)
        testList.foreach {
            Assertions.assertInstanceOf(classOf[SynchronizedObject[_]], _)
        }
        after()
    }

    @Test
    @Order(6)
    def clearList(): Unit = {
        before()
        testList.clear()
        Assertions.assertTrue(testList.isEmpty)
        after()
    }


}
