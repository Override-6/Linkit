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

package fr.linkit.api.connection.cache;

/**
 * An enum that will define how a synchronised cache will be opened by a {@link SharedCacheManager}.
 *
 * @see SharedCacheManager#getCache
 */
public enum CacheOpenBehavior {
    /**
     * Retrieves the cache or wait until it get opened on the targeted engine's {@link SharedCacheManager}
     */
    GET_OR_WAIT,
    /**
     * Retrieves the cache or open it (as an empty cache) if it is not opened on the targeted engine's {@link SharedCacheManager}
     * */
    GET_OR_OPEN,
    /**
     * Retrieves the cache or throw a {@link CacheOpenException} if it is not opened on the targeted engine's {@link SharedCacheManager}
     * */
    GET_OR_CRASH
}
