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
import fr.linkit.api.connection.network.{Engine, Network}
import fr.linkit.engine.connection.cache.SharedCacheDistantManager
import fr.linkit.engine.connection.cache.obj.invokation.ExecutorEngine

import java.sql.Timestamp
import scala.collection.mutable

class NetworkDataTrunk(network: Network) {

    private val engines = mutable.HashMap.empty[String, Engine]
    private val caches  = mutable.HashMap.empty[String, SharedCacheManager]
    val startUpDate: Timestamp = new Timestamp(System.currentTimeMillis())

    @MethodControl(value = BROADCAST)
    def newEngine(engineIdentifier: String): Engine = {
        if (engines.contains(engineIdentifier))
            throw new IllegalArgumentException("This engine already exists !")
        val current = ExecutorEngine.currentEngine
        val cache = {
            if (network.connectionEngine ne current) network.findCacheManager(engineIdentifier).get //TODO orElse throw
            else network.declareNewCacheManager(engineIdentifier)
        }
        val engine = new DefaultEngine(engineIdentifier, cache)
        engines.put(engineIdentifier, engine)
        engine
    }

    @MethodControl(value = BROADCAST_IF_ROOT_OWNER)
    def removeEngine(engine: Engine): Unit = engines -= engine.identifier

    def findCache(family: String): Option[SharedCacheManager] = caches.get(family)

    @MethodControl(value = BROADCAST)
    def addCacheManager(manager: SharedCacheManager): Unit = {
        if (caches.contains(manager.family))
            throw new Exception("Cache Manager already exists")
        caches.put(manager.family, manager)
    }

    def findEngine(identifier: String): Option[Engine] = engines.get(identifier)

    def listEngines: List[Engine] = engines.values.toList

    def countConnection: Int = engines.size

}
