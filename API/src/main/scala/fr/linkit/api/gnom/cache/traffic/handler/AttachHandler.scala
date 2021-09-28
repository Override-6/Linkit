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

package fr.linkit.api.gnom.cache.traffic.handler

import fr.linkit.api.application.network.Engine

/**
 * An handler that can get events when an engine attach or detach from the handled cache
 * And, if the handler handles a cache managed by a manager that handles his own caches (see [[fr.linkit.api.gnom.cache.SharedCacheManager]])
 * */
trait AttachHandler extends CacheHandler {

    /**
     * Called when a [[Engine]] is accepted (see [[inspectEngine()]]) and attaches to the handled cache.
     * This method is called on all handlers of a cache
     * @param engine the engine that attaches to this cache.
     * */
    def onEngineAttached(engine: Engine): Unit = ()

    /**
     * Controls if an engine is accepted or not.
     * This method is called only if the handler handles a cache where its manager handles itself.
     * @param engine the tested engine
     * @param requestedCacheType the Shared Cache class that the engine want to use.
     *                           The requestedCacheType is assignable to the current handled cache type.
     * @return None if there is no reason for why the engine is not accepted, Some[String]
     *         to specify why the engine is not accepted.
     * */
    def inspectEngine(engine: Engine, requestedCacheType: Class[_]): Option[String] = None //All engines accepted by default

    /**
     * Called when an [[Engine]] is detached.
     * This method is called on all handlers of a cache.
     * @param engine the engine that attaches to this cache.
     * */
    def onEngineDetached(engine: Engine): Unit = ()

}
