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

import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.repo.DefaultEngineObjectCenter
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

@TestInstance(Lifecycle.PER_CLASS)
class SynchronisedObjectTests {

    private val connection                          = EngineTests.testConnection
    private var repo: DefaultEngineObjectCenter[Serializable] = _

    @Test
    def openSharedCache(): Unit = {
        Assertions.assertNotNull(connection)
        val cache = connection.network.cache
        AppLogger.debug("Opening object repo with identifier '75'")
        val repo = cache.retrieveCache(75, DefaultEngineObjectCenter[Serializable]())
    }

}
