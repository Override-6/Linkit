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

package fr.linkit.api.gnom.cache;

/**
 * An enum that will define how the content of a synchronised cache will be retrieved by a {@link SharedCacheManager}.
 *
 * @see SharedCacheManager#attachToCache  
 */
public enum CacheSearchMethod {
    /**
     * Retrieves the cache content or wait until the cache get opened
     * by another machine or from another call of the {@link SharedCacheManager#attachToCache} with a method that let the cache be created.
     * {@link SharedCacheManager}
     */
    GET_OR_WAIT,
    /**
     * Retrieves the cache content or open it as empty if no cache was found. The cache content returned will be empty.
     * */
    GET_OR_OPEN,
    /**
     * Retrieves the cache or throw a {@link CacheOpenException} if it is not opened on the targeted {@link SharedCacheManager}
     * */
    GET_OR_CRASH

}
