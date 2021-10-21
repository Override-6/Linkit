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

package fr.linkit.client.test.sync

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.client.test.ClientLauncher
import fr.linkit.engine.test.Player
import fr.linkit.engine.test.sync.SimpleSynchronizedObjectsTest
import fr.linkit.engine.test.sync.SimpleSynchronizedObjectsTest.OverrideMotherPlayer
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Test, _}

import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable.ListBuffer

//noinspection AccessorLikeMethodIsUnit
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(classOf[OrderAnnotation])
class ClientSideSimpleListBufferTests extends SimpleSynchronizedObjectsTest {

    override protected lazy val app: ApplicationContext = {
        val random = ThreadLocalRandom.current()
        ClientLauncher.launch(Array("--identifier", random.nextInt().toString, "--raid-count", "1"))
    }
    private var testList           : ListBuffer[Player] = _
    private var testPlayer                              = OverrideMotherPlayer

    def before(): Unit = {
        AppLogger.info("Waiting for next step...")
        waitNextStep()
    }

    def after(): Unit = {
        AppLogger.info("Unlocking next step")
        unlockNextStep()
    }

    @Test
    @Order(0)
    def getEmptyListBuffer(): Unit = {
        //before()
        testList = testCache.findObject(1).orNull
        Assertions.assertNotNull(testList, "Could not find test list buffer.")
        Assertions.assertInstanceOf(classOf[ListBuffer[_]], testList)
        Assertions.assertInstanceOf(classOf[SynchronizedObject[_]], testList)
        Assertions.assertTrue(testList.isEmpty)
        //after()
    }

    @Test
    @Order(1)
    def addPlayer(): Unit = {
        //before()
        testList += testPlayer
        AppLogger.info(s"Added player $testPlayer")
        Assertions.assertTrue(testList.size == 1)
        testPlayer = testList.head
        Assertions.assertNotNull(testPlayer)
        Assertions.assertTrue(testPlayer == OverrideMotherPlayer)
        Assertions.assertTrue(testPlayer ne OverrideMotherPlayer)
        Assertions.assertInstanceOf(classOf[SynchronizedObject[_]], testPlayer)
        after()
    }

    @Test
    @Order(2)
    def checkPlayerModifications(): Unit = {
        before()
        Assertions.assertTrue(testList.size == 1)
        val player = testList.head
        Assertions.assertTrue(player != testPlayer)
        after()
    }

    @Test
    @Order(3)
    def reInjectPlayer(): Unit = {
        before()
        testList += testPlayer
        after()
    }

    @Test
    @Order(4)
    def reInjectPlayer100x(): Unit = {
        before()
        for (_ <- 0 to 100) {
            testList += testPlayer
        }
        after()
    }

    @Test
    @Order(5)
    def addRandomPlayer100x(): Unit = {
        before()
        val random = ThreadLocalRandom.current()
        for (_ <- 0 to 100) {
            testList += Player(random.nextInt(), ":c:c:cc:c:c", "damn", random.nextInt(), random.nextInt())
            Assertions.assertInstanceOf(classOf[SynchronizedObject[_]], testList.last)
        }
        after()
    }

    @Test
    @Order(6)
    def assertListCleared(): Unit = {
        before()
        Assertions.assertTrue(testList.isEmpty)
        after()
    }
}
