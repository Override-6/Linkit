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

package fr.linkit.core.connection.packet.serialization.procedures

import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.network.cache.CacheOpenBehavior
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.network.cache.puppet.{PuppetWrapper, CloudObjectRepository}
import fr.linkit.core.connection.packet.serialization.Procedure

object PuppetWrapperProcedure extends Procedure[PuppetWrapper[Serializable]] {

    private var failCount = 0

    override def beforeSerial(t: PuppetWrapper[Serializable], network: Network): Unit = {
        //NO-OP
    }

    override def afterDeserial(wrapper: PuppetWrapper[Serializable], network: Network): Unit = {
        val puppeteerDesc = wrapper.getPuppeteerDescription
        val family        = puppeteerDesc.cacheFamily
        val cacheID       = puppeteerDesc.cacheID
        network.getCacheManager(puppeteerDesc.cacheFamily).fold {
            AppLogger.warn(s"${wrapper.getClass.getName}: Received packet containing puppet that belongs to cache family '$family' which is not opened on this machine.")
            AppLogger.warn(s"Therefore, this object will be lost and will not be synchronised as it may was probably expected.")
            if (failCount % 10 == 0) {
                AppLogger.warn(s"Tip: You can open the cache manager '$family' then open a ${classOf[CloudObjectRepository].getSimpleName} with cache identifier '$cacheID")
                AppLogger.warn(s"     In order to retrieve this object.")
            }
            failCount += 1
        } { cache =>
            val repo = cache.getCache(cacheID, CloudObjectRepository, CacheOpenBehavior.GET_OR_CRASH)
            repo.initPuppetWrapper(wrapper)
        }
    }
}
