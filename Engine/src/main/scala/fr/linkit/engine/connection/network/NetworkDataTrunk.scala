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

package fr.linkit.engine.connection.network

import fr.linkit.api.connection.cache.SharedCacheManager
import fr.linkit.api.connection.cache.obj.behavior.annotation.BasicInvocationRule._
import fr.linkit.api.connection.cache.obj.behavior.annotation.MethodControl
import fr.linkit.api.connection.network.Engine

import java.sql.Timestamp
import scala.collection.mutable

class NetworkDataTrunk {

    private val engines = mutable.HashMap.empty[String, Engine]
    private val caches  = mutable.HashMap.empty[String, SharedCacheManager]
    val startUpDate: Timestamp = new Timestamp(System.currentTimeMillis())

    @MethodControl(value = BROADCAST, invokeOnly = true)
    def newEngine(engineIdentifier: String, cacheManager: SharedCacheManager): Engine = {
        val engine = new DefaultEngine(engineIdentifier, cacheManager)
        engines.put(engineIdentifier, engine)
        engine
    }

    @MethodControl(value = BROADCAST_IF_ROOT_OWNER, invokeOnly = true)
    def removeEngine(engine: Engine): Unit = engines -= engine.identifier

    def findCache(family: String): Option[SharedCacheManager]

    @MethodControl(value = BROADCAST_IF_ROOT_OWNER, invokeOnly = true)
    def addCacheManager(manager: SharedCacheManager): Unit = {
        if (caches.contains(manager.family))
            throw new Exception("Cache Manager already exists")
        caches.put(manager.family, manager)
    }

    def findEngine(identifier: String): Option[Engine] = engines.get(identifier)

    def listEngines: List[Engine] = engines.values.toList

}
