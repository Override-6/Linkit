/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.test.cache

import fr.linkit.api.gnom.cache.sync.ConnectedObjectCache
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.{CacheNotAcceptedException, SharedCacheFactory}
import fr.linkit.engine.gnom.cache.AbstractSharedCache
import org.junit.jupiter.api.{Assertions, Test}

class SharedCacheTest {

    import fr.linkit.test.TestEngine._

    private val cacheManagerClient = serverSideNetwork.attachToCacheManager("test")
    private val cacheManagerServer = serverSideNetwork.attachToCacheManager("test")

    @Test
    def testCacheClassClash(): Unit = {
        cacheManagerServer.attachToCache[ConnectedObjectCache[Object]](150)

        Assertions.assertThrows(classOf[CacheNotAcceptedException], () => cacheManagerClient.attachToCache(150, MySuperCache))
    }

    @Test
    def testCOCOpen(): Unit = {
        cacheManagerServer.attachToCache[ConnectedObjectCache[Object]](151)

        cacheManagerClient.attachToCache[ConnectedObjectCache[Object]](151)
    }

    @Test
    def testCOCOpenClassClash(): Unit = {
        cacheManagerServer.attachToCache[ConnectedObjectCache[A]](152)

        //FIXME Should fail because B is not castable to A
        Assertions.assertThrows(classOf[CacheNotAcceptedException], () => cacheManagerClient.attachToCache[ConnectedObjectCache[B]](152))
    }

    @Test
    def testCOCOpenClassChild(): Unit = {
        cacheManagerServer.attachToCache[ConnectedObjectCache[A]](152)

        Assertions.assertDoesNotThrow(() => cacheManagerClient.attachToCache[ConnectedObjectCache[C]](152))
    }

    class A
    class B

    class C extends A
    class D extends B

    private class MySuperCache(channel: CachePacketChannel) extends AbstractSharedCache(channel) {

    }

    private object MySuperCache extends SharedCacheFactory[MySuperCache] {
        override val targetClass: Class[MySuperCache] = classOf[MySuperCache]

        override def createNew(channel: CachePacketChannel): MySuperCache = new MySuperCache(channel)
    }

}
