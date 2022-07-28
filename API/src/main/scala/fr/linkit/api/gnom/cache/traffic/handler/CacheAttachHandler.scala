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

package fr.linkit.api.gnom.cache.traffic.handler

import fr.linkit.api.gnom.network.Engine

/**
 * An handler that can get events when an engine attach or detach from the handled cache
 * And, if the handler handles a cache managed by a manager that handles his own caches (see [[fr.linkit.api.gnom.cache.SharedCacheManager]])
 * */
trait CacheAttachHandler extends CacheHandler {


    /**
     * Controls if an engine can attach to the handled cache.
     * This method is called only on origin caches.
     * @param engine the tested engine
     * @param requestedCacheType the Shared Cache class that the engine want to use.
     *                           The requestedCacheType is assignable to the current handled cache type.
     * @return None if there is no reason for why the engine is not accepted, Some[String]
     *         to specify why the engine is not accepted.
     * */
    def inspect(engine: Engine, cacheClass: Class[_], engineCacheType: Class[_]): Option[String] = {
        if (cacheClass eq engineCacheType) None
        else Some(s"Requested cache class is not ${cacheClass.getName} (received: ${engineCacheType.getName}).")
    }

    /**
     * Called when a [[Engine]] is accepted (see [[inspect()]]) and attaches to the handled cache.
     * This method is called on all handlers of a cache
     * @param engine the engine that attaches to this cache.
     * */
    def onEngineAttached(engine: Engine): Unit = ()

    /**
     * Called when an [[Engine]] is detached.
     * This method is called on all handlers of a cache.
     * @param engine the engine that attaches to this cache.
     * */
    def onEngineDetached(engine: Engine): Unit = ()

}
